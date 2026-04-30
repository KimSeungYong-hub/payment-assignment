package com.practice.paymentassignment;

import com.practice.paymentassignment.domain.merchant.Merchant;
import com.practice.paymentassignment.domain.merchant.service.MerchatService;
import com.practice.paymentassignment.domain.payment.PaymentRequestEntity;
import com.practice.paymentassignment.domain.user.User;
import com.practice.paymentassignment.domain.user.UserService;
import com.practice.paymentassignment.domain.wallet.WalletService;
import com.practice.paymentassignment.dto.PaymentDto;
import com.practice.paymentassignment.exception.InsufficientBalanceException;
import com.practice.paymentassignment.exception.PaymentExpiredException;
import com.practice.paymentassignment.domain.payment.service.PaymentService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

@RequiredArgsConstructor
@Service
@Transactional(readOnly = true)
public class PaymentUseCase {
    private final PaymentService paymentService;
    private final UserService userService;
    private final MerchatService merchatService;
    private final WalletService walletService;

    @Transactional
    public PaymentDto.Prepare.Response readyPayment(PaymentDto.Prepare.Request request, String idempotencyKey) {
        Merchant merchant = merchatService.findMerchant(request.getMerchantId());
        return paymentService.savePaymentRequest(merchant, idempotencyKey);
    }

    @Transactional
    public PaymentDto.Approve.Response confirmPayment(PaymentDto.Approve.Request request, Long userId) {
        PaymentRequestEntity paymentRequest = paymentService.findPaymentRequestWithLock(request.getPaymentId());
        BigDecimal totalAmount = paymentRequest.getTotalAmount();
        User user = userService.findUser(userId);

        try{
            paymentRequest.verifyCanBeApproved(request.getPaymentId(), request.getAmount());
            walletService.deduct(userId, totalAmount);
            paymentService.recordSuccess(paymentRequest, user, totalAmount);
            return PaymentDto.Approve.Response.of(true, "결제가 완료되었습니다.");
        }catch (InsufficientBalanceException|PaymentExpiredException e){
            //Propagation.REQUIRES_NEW 메서드 데드락 위험성?? + db 커넥션 2개 소모됨
            paymentService.recordFailure(paymentRequest, user, totalAmount, e.getMessage());
            throw e;
        }
    }

}
