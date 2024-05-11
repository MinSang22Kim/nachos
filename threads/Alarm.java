package nachos.threads;

import nachos.machine.*;

import java.util.Iterator;
import java.util.Vector;

/**
 * Uses the hardware timer to provide preemption, and to allow threads to sleep
 * until a certain time.
 */
public class Alarm {
    /**
     * Allocate a new Alarm. Set the machine's timer interrupt handler to this
     * alarm's callback.
     *
     * <p><b>Note</b>: Nachos will not function correctly with more than one
     * alarm.
     */
    public Alarm() {
        Machine.timer().setInterruptHandler(new Runnable() {
            public void run() { timerInterrupt(); }
        });
    }

    private Vector<KThread> waitingThreadQueue = new Vector<>(); // block된 스레드 리스트
    private Vector<Long> wakeTimeQueue = new Vector<>(); // wakeTime 리스트

    /**
     * The timer interrupt handler. This is called by the machine's timer
     * periodically (approximately every 500 clock ticks). Causes the current
     * thread to yield, forcing a context switch if there is another thread
     * that should be run.
     */
    public void timerInterrupt() {

        long nowTime = Machine.timer().getTime(); // 현재 시간을 nowTime 변수에 저장
        boolean interruptStatus = Machine.interrupt().disable(); // 스레드의 인터럽트 비활성화

        Iterator<KThread> threadIterator = waitingThreadQueue.iterator();
        Iterator<Long> timeIterator = wakeTimeQueue.iterator();

        while (threadIterator.hasNext() && timeIterator.hasNext()) { // 다음 스레드가 있는 경우에만 반복문 진입

            KThread thread = threadIterator.next();
            long time = timeIterator.next();

            if (time < nowTime) { // 대기큐에 있는 스레드의 wakeTime이 현재 시간보다 작은 경우
                thread.ready(); // 조건을 만족하는 경우 해당 스레드는 ready 상태로 전이됨
                threadIterator.remove(); // ready 상태로 세팅된 스레드는 대기 큐에서 제거
                timeIterator.remove(); // ready 상태로 세팅된 스레드의 wakeTime도 큐에서 제거
            }
        }
        KThread.currentThread().yield();
        Machine.interrupt().restore(interruptStatus);
    }

    /**
     * Put the current thread to sleep for at least <i>x</i> ticks,
     * waking it up in the timer interrupt handler. The thread must be
     * woken up (placed in the scheduler ready set) during the first timer
     * interrupt where
     *
     * <p><blockquote>
     * (current time) >= (WaitUntil called time)+(x)
     * </blockquote>
     *
     * @param	x	the minimum number of clock ticks to wait.
     *
     * @see	nachos.machine.Timer#getTime()
     */

    public void waitUntil(long x) {
        // for now, cheat just to get something working (busy waiting is bad)

        if(x<=0) return; // x Timer tick이 0 또는 음수인 경우 기다리지 않고 즉시 반환

        long wakeTime = Machine.timer().getTime() + x; // 현재시간 + x = 스레드 깨어날 시간
        boolean interruptStatus = Machine.interrupt().disable(); // 스레드의 인터럽트 비활성화

        waitingThreadQueue.addElement(KThread.currentThread()); // waitingThreadQueue에 스레드 추가
        wakeTimeQueue.add(wakeTime); // wakeTimeQueue에 대기시간 추가

        KThread.currentThread().sleep(); // waitUntil 메소드를 호출한 현재 스레드를 block시킴
        Machine.interrupt().restore(interruptStatus); // 인터럽트 상태 복원

        // 기존 코드 삭제
        //	while (wakeTime > Machine.timer().getTime())
        //	    KThread.yield();
    }
}
