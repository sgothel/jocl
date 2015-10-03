/*
 * Created on Tuesday, May 03 2011
 */
package com.jogamp.opencl.util.concurrent;

import com.jogamp.common.util.InterruptSource;
import com.jogamp.opencl.CLCommandQueue;
import com.jogamp.opencl.CLDevice;
import com.jogamp.opencl.CLResource;
import com.jogamp.opencl.util.CLMultiContext;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;

/**
 * A multithreaded, fixed size pool of OpenCL command queues.
 * It serves as a multiplexer distributing tasks over N queues usually run on N devices.
 * The usage of this pool is similar to {@link ExecutorService} but it uses {@link CLTask}s
 * instead of {@link Callable}s and provides a per-queue context for resource sharing across all tasks of one queue.
 * @author Michael Bien
 */
public class CLCommandQueuePool<C extends CLQueueContext> implements CLResource {

    private List<CLQueueContext> contexts;
    private ExecutorService excecutor;
    private FinishAction finishAction = FinishAction.DO_NOTHING;
    private boolean released;

    private CLCommandQueuePool(final CLQueueContextFactory<C> factory, final Collection<CLCommandQueue> queues) {
        this.contexts = initContexts(queues, factory);
        initExecutor();
    }

    private List<CLQueueContext> initContexts(final Collection<CLCommandQueue> queues, final CLQueueContextFactory<C> factory) {
        final List<CLQueueContext> newContexts = new ArrayList<CLQueueContext>(queues.size());

        int index = 0;
        for (final CLCommandQueue queue : queues) {

            CLQueueContext old = null;
            if(this.contexts != null && !this.contexts.isEmpty()) {
                old = this.contexts.get(index++);
                old.release();
            }

            newContexts.add(factory.setup(queue, old));
        }
        return newContexts;
    }

    private void initExecutor() {
        this.excecutor = Executors.newFixedThreadPool(contexts.size(), new QueueThreadFactory(contexts));
    }

    public static <C extends CLQueueContext> CLCommandQueuePool<C> create(final CLQueueContextFactory<C> factory, final CLMultiContext mc, final CLCommandQueue.Mode... modes) {
        return create(factory, mc.getDevices(), modes);
    }

    public static <C extends CLQueueContext> CLCommandQueuePool<C> create(final CLQueueContextFactory<C> factory, final Collection<CLDevice> devices, final CLCommandQueue.Mode... modes) {
        final List<CLCommandQueue> queues = new ArrayList<CLCommandQueue>(devices.size());
        for (final CLDevice device : devices) {
            queues.add(device.createCommandQueue(modes));
        }
        return create(factory, queues);
    }

    public static <C extends CLQueueContext> CLCommandQueuePool<C> create(final CLQueueContextFactory<C> factory, final Collection<CLCommandQueue> queues) {
        return new CLCommandQueuePool<C>(factory, queues);
    }

    /**
     * Submits this task to the pool for execution returning its {@link Future}.
     * @see ExecutorService#submit(java.util.concurrent.Callable)
     */
    public <R> Future<R> submit(final CLTask<? super C, R> task) {
        return excecutor.submit(new TaskWrapper<C,R>(task, finishAction));
    }

    /**
     * Submits all tasks to the pool for execution and returns their {@link Future}.
     * Calls {@link #submit(com.jogamp.opencl.util.concurrent.CLTask)} for every task.
     */
    public <R> List<Future<R>> submitAll(final Collection<? extends CLTask<? super C, R>> tasks) {
        final List<Future<R>> futures = new ArrayList<Future<R>>(tasks.size());
        for (final CLTask<? super C, R> task : tasks) {
            futures.add(submit(task));
        }
        return futures;
    }

    /**
     * Submits all tasks to the pool for immediate execution (blocking) and returns their {@link Future} holding the result.
     * @see ExecutorService#invokeAll(java.util.Collection)
     */
    public <R> List<Future<R>> invokeAll(final Collection<? extends CLTask<? super C, R>> tasks) throws InterruptedException {
        final List<TaskWrapper<C, R>> wrapper = wrapTasks(tasks);
        return excecutor.invokeAll(wrapper);
    }

    /**
     * Submits all tasks to the pool for immediate execution (blocking) and returns their {@link Future} holding the result.
     * @see ExecutorService#invokeAll(java.util.Collection, long, java.util.concurrent.TimeUnit)
     */
    public <R> List<Future<R>> invokeAll(final Collection<? extends CLTask<? super C, R>> tasks, final long timeout, final TimeUnit unit) throws InterruptedException {
        final List<TaskWrapper<C, R>> wrapper = wrapTasks(tasks);
        return excecutor.invokeAll(wrapper, timeout, unit);
    }

    private <R> List<TaskWrapper<C, R>> wrapTasks(final Collection<? extends CLTask<? super C, R>> tasks) {
        final List<TaskWrapper<C, R>> wrapper = new ArrayList<TaskWrapper<C, R>>(tasks.size());
        for (final CLTask<? super C, R> task : tasks) {
            if(task == null) {
                throw new NullPointerException("at least one task was null");
            }
            wrapper.add(new TaskWrapper<C, R>(task, finishAction));
        }
        return wrapper;
    }

    /**
     * Switches the context of all queues - this operation can be expensive.
     * Blocks until all tasks finish and sets up a new context for all queues.
     * @return this
     */
    public CLCommandQueuePool<C> switchContext(final CLQueueContextFactory<C> factory) {

        excecutor.shutdown();
        finishQueues(); // just to be sure

        contexts = initContexts(getQueues(), factory);
        initExecutor();
        return this;
    }

    /**
     * Calls {@link CLCommandQueue#flush()} on all queues.
     */
    public void flushQueues() {
        for (final CLQueueContext context : contexts) {
            context.queue.flush();
        }
    }

    /**
     * Calls {@link CLCommandQueue#finish()} on all queues.
     */
    public void finishQueues() {
        for (final CLQueueContext context : contexts) {
            context.queue.finish();
        }
    }

    /**
     * Releases all queues.
     */
    @Override
    public void release() {
        if(released) {
            throw new RuntimeException(getClass().getSimpleName()+" already released");
        }
        released = true;
        excecutor.shutdown();
        for (final CLQueueContext context : contexts) {
            context.queue.finish().release();
            context.release();
        }
    }

    /**
     * Returns the command queues used in this pool.
     */
    public List<CLCommandQueue> getQueues() {
        final List<CLCommandQueue> queues = new ArrayList<CLCommandQueue>(contexts.size());
        for (final CLQueueContext context : contexts) {
            queues.add(context.queue);
        }
        return queues;
    }

    /**
     * Returns the size of this pool (number of command queues).
     */
    public int getSize() {
        return contexts.size();
    }

    public FinishAction getFinishAction() {
        return finishAction;
    }

    @Override
    public boolean isReleased() {
        return released;
    }

    /**
     * Sets the action which is run after every completed task.
     * This is mainly intended for debugging, default value is {@link FinishAction#DO_NOTHING}.
     */
    public void setFinishAction(final FinishAction action) {
        this.finishAction = action;
    }

    @Override
    public String toString() {
        return getClass().getSimpleName()+" [queues: "+contexts.size()+" on finish: "+finishAction+"]";
    }

    private static class QueueThreadFactory implements ThreadFactory {

        private final List<CLQueueContext> context;
        private int index;

        private QueueThreadFactory(final List<CLQueueContext> queues) {
            this.context = queues;
            this.index = 0;
        }

        public synchronized Thread newThread(final Runnable runnable) {

            final SecurityManager sm = System.getSecurityManager();
            final ThreadGroup group = (sm != null) ? sm.getThreadGroup() : Thread.currentThread().getThreadGroup();

            final CLQueueContext queue = context.get(index);
            final QueueThread thread = new QueueThread(group, runnable, queue, index++);
            thread.setDaemon(true);

            return thread;
        }

    }

    private static class QueueThread extends InterruptSource.Thread {
        private final CLQueueContext context;
        public QueueThread(final ThreadGroup group, final Runnable runnable, final CLQueueContext context, final int index) {
            super(group, runnable, "queue-worker-thread-"+index+"["+context+"]");
            this.context = context;
        }
    }

    private static class TaskWrapper<C extends CLQueueContext, R> implements Callable<R> {

        private final CLTask<? super C, R> task;
        private final FinishAction mode;

        public TaskWrapper(final CLTask<? super C, R> task, final FinishAction mode) {
            this.task = task;
            this.mode = mode;
        }

        public R call() throws Exception {
            final CLQueueContext context = ((QueueThread)Thread.currentThread()).context;
            // we make sure to only wrap tasks on the correct kind of thread, so this
            // shouldn't fail (trying to genericize QueueThread properly becomes tricky)
            @SuppressWarnings("unchecked")
            final
            R result = task.execute((C)context);
            if(mode.equals(FinishAction.FLUSH)) {
                context.queue.flush();
            }else if(mode.equals(FinishAction.FINISH)) {
                context.queue.finish();
            }
            return result;
        }

    }

    /**
     * The action executed after a task completes.
     */
    public enum FinishAction {

        /**
         * Does nothing, the task is responsible to make sure all computations
         * have finished when the task finishes
         */
        DO_NOTHING,

        /**
         * Flushes the queue on task completion.
         */
        FLUSH,

        /**
         * Finishes the queue on task completion.
         */
        FINISH
    }

}
