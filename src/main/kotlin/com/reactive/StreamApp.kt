package com.reactive

import com.reactive.publishers.StreamPublisher
import org.reactivestreams.Subscriber
import org.reactivestreams.Subscription
import java.util.concurrent.Callable
import java.util.function.Supplier
import java.util.stream.Stream


fun main() {
//    StreamPublisher<Int>(Supplier {
//        Stream.of(1, 2, 3, 4, 5)
//    }).subscribe(IntStreamSubscriber)

    StreamPublisher<Int>(Supplier {
        Stream.of(1, 2, 3, 4, 5)
    }).map {
        it*2
    }.map {
        "result: ${it*2}"
    }.map {
        println(it)
        it
    }.flatMap {
        asyncCall()
    }.map {
        println(it)
    }.subscribe()
}

object IntStreamSubscriber: Subscriber<Int> {
    override fun onComplete() {
        println("Completed")
    }

    override fun onSubscribe(s: Subscription) {
        s.request(1)
    }

    override fun onNext(t: Int) {
        println(t)
    }

    override fun onError(t: Throwable) {
        println(t)
    }
}

fun asyncCall(): StreamPublisher<Int> {
    return StreamPublisher<Int>( Supplier {
        Stream.of(7,8,9)
    } )
}