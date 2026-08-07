package kohgylw.kiftd.server.util;

import java.io.BufferedOutputStream;
import java.io.IOException;
import java.io.OutputStream;

import jakarta.servlet.http.HttpSession;

/**
 * 
 * <h2>带限流作用的缓存输出流</h2>
 * <p>
 * 该工具是对普通缓存输出流BufferedOutputStream的升级，能够将输出速度限制在指定速率之内，便于系统管理输出带宽。
 * </p>
 * <p>
 * 特别提示：该类中仅有write(byte[] b, int off, int len)方法具备限速功能，其余方法则不具备。因此，如果您希望控制输出速率，
 * 请使用（且仅使用）该方法实现输出操作。
 * </p>
 * 
 * @author 青阳龙野(kohgylw)
 * @version 1.0
 */
public class VariableSpeedBufferedOutputStream extends BufferedOutputStream {

	private long maxRate;// 该实例的最大输出限速，以B/s为单位。
	private HttpSession session;// 该实例所用的用户会话对象，用于线程锁。
	private long writtenLength;// 一秒之内已经写出的数据长度，以B为单位
	private long startTime;// 计时起始时间，以毫秒为单位

	/**
	 * 
	 * <h2>创建一个限速缓存输出流实例</h2>
	 * <p>
	 * 请使用该方法构造一个实例，然后开始使用。必须按照参数说明给定正确的参数以确保该实例能够正常发挥作用。
	 * </p>
	 * 
	 * @author 青阳龙野(kohgylw)
	 * @param out     java.io.OutputStream 原始输出流，传入方法与普通的BufferedOutputStream构造器相同
	 * @param maxRate long 每秒最大可输出的数据数量，以B为单位。例如：传入1024就代表该实例的最大输出限速为1KB/s
	 * @param session javax.servlet.http.HttpSession 传入用户会话。该对象用于锁定输出操作，从而确保当用户开启多个
	 *                下载任务时，总的最大下载速率仍不会超过限速值
	 */
	public VariableSpeedBufferedOutputStream(OutputStream out, long maxRate, HttpSession session) {
		super(out);
		this.maxRate = maxRate;
		this.session = session;
		this.writtenLength = 0;
		this.startTime = System.currentTimeMillis();
	}

	/**
	 * 
	 * <h2>升级的write(byte[] b, int off, int len)方法</h2>
	 * <p>
	 * 该方法将按照 <strong>构造器中传入的最大输出速率限制</strong>
	 * 输出数据。使用方法与原BufferedOutputStream类中定义的方法完全相同。
	 * </p>
	 * 
	 * @author 青阳龙野(kohgylw)
	 * @param b   byte[] 数据数组
	 * @param off int 数据的起始下标
	 * @param len int 数据在数组中的长度
	 */
	public void write(byte[] b, int off, int len) throws IOException {
		if (maxRate > 0) {
			// 限速输出：同一会话下的所有下载任务共享一个速率预算（以 session 为监视器）。
			// 预算用尽时通过 session.wait(...) 等待窗口重置——wait 会释放监视器，
			// 避免慢速下载任务长时间独占锁导致同账户其它并发任务被阻塞（PERF-003）。
			int startIndex = off;// 记录当前应读数组的起始位置
			int surplusLength = len;// 记录数组中应写的剩余数据量
			while (surplusLength > 0) {
				synchronized (session) {
					if (writtenLength >= maxRate) {
						// 本秒预算已用尽：等待窗口重置（wait 期间释放监视器，其它任务可继续竞争）
						long remain = 1000 - (System.currentTimeMillis() - startTime);
						// wait 可能被同一会话其它任务的 notifyAll 提前唤醒（或发生伪唤醒），
						// 循环重算剩余等待时间，确保窗口真正到期后才重置预算，避免限速失真
						while (remain > 0) {
							try {
								session.wait(remain);
							} catch (InterruptedException e) {
								// 如果收到中断指令，那么就响应中断
								Thread.currentThread().interrupt();
							}
							remain = 1000 - (System.currentTimeMillis() - startTime);
						}
						// 唤醒同会话等待窗口重置的其它下载任务，避免它们被迫等待满一个完整窗口
						session.notifyAll();
						writtenLength = 0;
						startTime = System.currentTimeMillis();
						continue;
					}
					// 如果尚未开始计量一秒内的写出量，则记录写出前的毫秒值
					if (writtenLength == 0) {
						startTime = System.currentTimeMillis();
					}
					// 计算此秒之内最多还能写出多少数据，并取出本批可写出量
					long shouldWriteLength = maxRate - writtenLength;
					int n = (int) Math.min(shouldWriteLength, (long) surplusLength);
					super.write(b, startIndex, n);
					startIndex += n;
					writtenLength += n;
					surplusLength -= n;
				}
			}
		} else if (maxRate < 0) {// 如果限速值为负数，则不限速输出
			super.write(b, off, len);
		} else {
			// 如果限速值为0，那肯定是限速设置有误造成的。
			throw new IllegalArgumentException("Error:invalid maximum download rate value.");
		}
	}
}
