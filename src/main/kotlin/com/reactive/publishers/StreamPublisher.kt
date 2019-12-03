package com.reactive.publishers

import org.reactivestreams.Publisher
import org.reactivestreams.Subscriber
import org.reactivestreams.Subscription
import java.lang.NullPointerException
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicLong
import java.util.concurrent.atomic.AtomicReference
import java.util.function.Supplier
import java.util.stream.Stream


open class StreamPublisher<T> protected constructor(): Publisher<T> {
    var streamSupplier: Supplier<Stream<T>>? = null

    constructor(streamSupplier: Supplier<Stream<T>>): this() {
        this.streamSupplier = streamSupplier
    }

    override fun subscribe(subscriber: Subscriber<in T>?) {
        if(subscriber == null) {
            throw NullPointerException("subscriber can not be null")
        }
        val streamSubscription = StreamSubscription(subscriber)
        subscriber.onSubscribe(streamSubscription)
        streamSubscription.doOnSubscribed()
    }

    fun <R> map( mapper: (T) -> R ): StreamPublisher<R> {
        return StreamMapper(this, mapper)
    }

    fun <R> flatMap( mapper: (T) -> StreamPublisher<R> ): StreamPublisher<R> {
        return FlatMapStreamMapper(this, mapper)
    }

    open fun subscribe() {

    }


    inner class StreamSubscription<T> private constructor(): Subscription {

        private val error = AtomicReference<Throwable>()
        private lateinit var iterator: Iterator<*>
        private lateinit var subscriber: Subscriber<T>
        private val demand:AtomicLong = AtomicLong()

        constructor(subscriber: Subscriber<T>): this() {
            this.subscriber = subscriber
            try {
                iterator = streamSupplier!!.get().iterator()
            } catch (e: Throwable) {
                error.set(e)
            }
        }

        private val isTerminated = AtomicBoolean(false)

        override fun cancel() {
            terminate()
        }

        override fun request(n: Long) {
            if (isIllegalSubscription(n)) {
                subscriber.onError(IllegalArgumentException("negative subscription request"));
                return;
            }

            if (demand.getAndAdd(n) > 0) {
                return;
            }

            while(demand.get() > 0 && iterator.hasNext() && !isTerminated()) {
                try {
                    subscriber.onNext(iterator.next() as T)
                    demand.decrementAndGet()
                } catch (e: Throwable) {
                    if(!terminate()) {
                        subscriber.onError(e)
                    }
                }
            }
            if(!iterator.hasNext() && !terminate()) {
                subscriber.onComplete()
            }
        }

        private fun isIllegalSubscription(n: Long) = n <= 0 && !terminate()

        private fun terminate(): Boolean = isTerminated.getAndSet(true)

        private fun isTerminated(): Boolean  = isTerminated.get()

        fun doOnSubscribed() {
            val throwable = error.get()
            if (throwable != null && !terminate()) {
                subscriber.onError(throwable)
            }
        }
    }
}

class StreamMapper<X, Y>(private val actual: StreamPublisher<X>, private val mapper: (X) -> Y): StreamPublisher<Y>() {

    override fun subscribe() {
        actual.subscribe(LambdaSubscriber(null, mapper))
    }

    override fun subscribe(subscriber: Subscriber<in Y>?) {
        actual.subscribe(LambdaSubscriber(subscriber, mapper))
    }


}

class LambdaSubscriber<T,R>(private val subscriber: Subscriber<in R>?, private val mapper: (T) -> R) : Subscriber<T> {
    override fun onComplete() {
        subscriber?.onComplete()
    }

    override fun onSubscribe(s: Subscription) {
        subscriber?.onSubscribe(s)
        s.request(Long.MAX_VALUE)
    }

    override fun onNext(t: T) {
        val r = mapper.invoke(t)
        subscriber?.onNext(r)
    }

    override fun onError(t: Throwable?) {
        throw t!!
    }

}

class FlatMapStreamMapper<T, R>(private val actual: StreamPublisher<T>, private val flatMapper: (T) -> StreamPublisher<R>)
    : StreamPublisher<R>() {
    override fun subscribe() {
        actual.subscribe(LambdaFlatMapSubscriber(null, flatMapper))
    }
    override fun subscribe(subscriber: Subscriber<in R>?) {
        actual.subscribe(LambdaFlatMapSubscriber(subscriber, flatMapper))
    }
}

class LambdaFlatMapSubscriber<T, R>(private val subscriber: Subscriber<in R>?, private val flatMapper: (T) -> StreamPublisher<R>) : Subscriber<T> {
    override fun onComplete() {
        subscriber?.onComplete()
    }

    override fun onSubscribe(s: Subscription) {
        subscriber?.onSubscribe(s)
        s.request(Long.MAX_VALUE)
    }

    override fun onNext(t: T) {
        val r = flatMapper.invoke(t)
        if(subscriber!=null)r.subscribe(subscriber)
    }

    override fun onError(t: Throwable?) {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

}





