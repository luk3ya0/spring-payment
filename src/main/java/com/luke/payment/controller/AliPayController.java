package com.luke.payment.controller;

import com.alipay.api.AlipayApiException;
import com.alipay.api.AlipayConstants;
import com.alipay.api.internal.util.AlipaySignature;
import com.luke.payment.entity.OrderInfo;
import com.luke.payment.service.AliPayService;
import com.luke.payment.service.OrderInfoService;
import com.luke.payment.vo.R;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.env.Environment;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import java.math.BigDecimal;
import java.util.Map;

@Slf4j
@CrossOrigin
@RestController
@Api(tags = "网站支付宝支付")
@RequestMapping("/api/ali-pay")
public class AliPayController {
    @Resource
    AliPayService aliPayService;

    @Resource
    private Environment config;

    @Resource
    private OrderInfoService orderInfoService;

    @ApiOperation("统一收单下单并支付页面接口的调用")
    @PostMapping("/trade/page/pay/{productId}")
    public R tradePagePay(@PathVariable Long productId) {
        log.info("统一收单下单并支付页面接口的调用");

        String formStr = aliPayService.tradeCreate(productId);

        return R.ok().data("formStr", formStr);
    }

    @ApiOperation("支付宝支付通知")
    @PostMapping("/trade/notify")
    public String tradeNotify(@RequestParam Map<String, String> params) {
        log.info("支付宝-支付通知正在执行");
        log.info("通知参数 ===> {}", params);

        String result = "failure";

        try {
            boolean singnVerified = AlipaySignature.rsaCheckV1(
                    params,
                    config.getProperty("alipay.alipay-public-key"),
                    AlipayConstants.CHARSET_UTF8,
                    AlipayConstants.SIGN_TYPE_RSA2);

            if (!singnVerified) {
                log.error("异步通知验签失败!");

                return result;
            }

            log.info("支付成功异步通知验签成功!");

            String outTradeNo = params.get("out_trade_no");
            OrderInfo orderInfo = orderInfoService.getOrderByOrderNo(outTradeNo);

            if (orderInfo == null) {
                log.error("订单不存在");

                return result;
            }

            String totalAmount = params.get("total_amount");
            int totalAmountInt = new BigDecimal(totalAmount).multiply(new BigDecimal("100")).intValue();
            int totalFeeInt = orderInfo.getTotalFee();

            if (totalFeeInt != totalAmountInt) {
                log.error("订单金额校验失败");

                return result;
            }

            String sellerId = params.get("seller_id");
            String sellerIdProperty = config.getProperty("alipay.seller-id");

            if (!sellerIdProperty.equals(sellerId)) {
                log.error("商家 pid 校验失败");

                return result;
            }

            String appId = params.get("app_id");
            String appIdProperty = config.getProperty("alipay.app-id");

            if (!appId.equals(appIdProperty)) {
                log.error("appId 校验失败");

                return result;
            }

            String tradeStatus = params.get("trade_status");

            if (!"TRADE_SUCCESS".equals(tradeStatus)) {
                log.error("支付未成功");

                return result;
            }

            aliPayService.processOrder(params);

            result = "success";
        } catch (AlipayApiException e) {
            log.error(e.getMessage());
        }

        return result;
    }

    @ApiOperation("用户取消订单")
    @PostMapping("/trade/close/{orderNo}")
    public R cancel(@PathVariable String orderNo) {
        log.info("取消订单");

        aliPayService.cancelOrder(orderNo);

        return R.ok().setMessage("订单已取消");
    }

    @GetMapping("/trade/query/{orderNo}")
    public R queryOrder(@PathVariable String orderNo) throws Exception {
        log.info("Query Order");

        String result = aliPayService.queryOrder(orderNo);

        return R.ok().setMessage("Query successfully").data("result", result);
    }

    @ApiOperation("申请退款")
    @PostMapping("/trade/refund/{orderNo}/{reason}")
    public R refunds(@PathVariable String orderNo, @PathVariable String reason) {
        log.info("申请退款");
        aliPayService.refund(orderNo, reason);

        return R.ok();
    }

    @ApiOperation("查询退款")
    @GetMapping("/trade/fastpay/refund/{orderNo}")
    public R queryRefund(@PathVariable String orderNo) throws Exception {
        log.info("查询退款");

        String result = aliPayService.queryRefund(orderNo);

        return R.ok().setMessage("查询成功").data("result", result);
    }

    @ApiOperation("获取账单 url")
    @GetMapping("/bill/downloadurl/query/{billDate}/{type}")
    public R queryTradeBill(
            @PathVariable String billDate,
            @PathVariable String type) {
        log.info("获取账单 url");

        String downloadUrl = aliPayService.queryBill(billDate, type);

        return R.ok().setMessage("获取账单 url 成功").data("downloadUrl", downloadUrl);
    }
}
