import type { TransferTrendPoint } from '../types';

/** 把一组数值缩放成 SVG polyline 坐标；空值和全零数据也能稳定显示。 */
function buildPoints(values: number[], width: number, height: number): string {
  const max = Math.max(...values, 1);
  const horizontalStep = values.length > 1 ? width / (values.length - 1) : width;
  return values
    .map((value, index) => {
      const x = index * horizontalStep;
      const y = height - (value / max) * (height - 12) - 6;
      return `${x.toFixed(1)},${y.toFixed(1)}`;
    })
    .join(' ');
}

/**
 * 轻量级原生 SVG 趋势图，避免为一个小图表引入大型图表库。
 * aria-label 提供文字摘要，颜色之外还使用不同线型区分已完成与处理中。
 */
export function TrendChart({ trend }: { trend: TransferTrendPoint[] }) {
  const completed = trend.map((point) => point.completedCount);
  const processing = trend.map((point) => point.processingCount);
  const width = 920;
  const height = 250;
  const labelIndexes = [0, 6, 12, 18, 23].filter((index) => index < trend.length);

  return (
    <div className="trend-chart">
      <div className="chart-legend" aria-hidden="true">
        <span><i className="legend-dot legend-dot--info" />已完成</span>
        <span><i className="legend-line legend-line--warning" />处理中</span>
      </div>
      <svg
        viewBox={`0 0 ${width} ${height + 32}`}
        role="img"
        aria-label={`近 24 小时已完成 ${completed.reduce((sum, value) => sum + value, 0)} 笔，处理中 ${processing.reduce((sum, value) => sum + value, 0)} 笔`}
      >
        <defs>
          <linearGradient id="trend-fill" x1="0" y1="0" x2="0" y2="1">
            <stop offset="0%" stopColor="#38bdf8" stopOpacity="0.22" />
            <stop offset="100%" stopColor="#38bdf8" stopOpacity="0" />
          </linearGradient>
        </defs>
        {[0, 1, 2, 3, 4].map((line) => (
          <line
            key={line}
            x1="0"
            x2={width}
            y1={(height / 4) * line}
            y2={(height / 4) * line}
            className="chart-gridline"
          />
        ))}
        {trend.length > 0 && (
          <>
            <polygon
              points={`0,${height} ${buildPoints(completed, width, height)} ${width},${height}`}
              fill="url(#trend-fill)"
            />
            <polyline points={buildPoints(completed, width, height)} className="chart-line chart-line--completed" />
            <polyline points={buildPoints(processing, width, height)} className="chart-line chart-line--processing" />
          </>
        )}
        {labelIndexes.map((index) => (
          <text key={index} x={(width / 23) * index} y={height + 24} className="chart-label" textAnchor={index === 0 ? 'start' : index === 23 ? 'end' : 'middle'}>
            {new Date(trend[index].hour).toLocaleTimeString('zh-CN', { hour: '2-digit', minute: '2-digit', hour12: false })}
          </text>
        ))}
      </svg>
    </div>
  );
}
