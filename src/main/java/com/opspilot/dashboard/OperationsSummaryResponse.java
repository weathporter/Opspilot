package com.opspilot.dashboard;

import com.opspilot.transfer.TransferResponse;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 运行总览的服务端读模型，所有数字都来自当前 MySQL 数据而非前端演示常量。
 *
 * @param generatedAt 汇总生成时间，便于判断页面数据是否陈旧
 * @param environment 当前运行环境
 * @param version 当前应用版本
 * @param accountCount 账户总数
 * @param totalBalance 账户余额总和
 * @param transferCount 转账订单总数
 * @param completedTransferCount 已完成转账数
 * @param completedTransferVolume 已完成转账金额总和
 * @param successRate 已完成订单占比
 * @param trend 近 24 个整点的交易趋势
 * @param recentTransfers 最近 6 笔转账
 */
public record OperationsSummaryResponse(
        LocalDateTime generatedAt,
        String environment,
        String version,
        long accountCount,
        BigDecimal totalBalance,
        long transferCount,
        long completedTransferCount,
        BigDecimal completedTransferVolume,
        double successRate,
        List<TransferTrendPoint> trend,
        List<TransferResponse> recentTransfers
) {
}
