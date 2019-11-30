package com.reactive.publishers

import org.reactivestreams.Publisher
import org.reactivestreams.tck.PublisherVerification
import org.reactivestreams.tck.TestEnvironment
import java.util.function.Supplier
import java.util.function.UnaryOperator
import java.util.stream.Stream

class StreamPublisherTest: PublisherVerification<Int>(TestEnvironment()) {

    override fun createPublisher(elements: Long): Publisher<Int> {
        return StreamPublisher<Int>(Supplier {
            Stream.iterate(0, UnaryOperator.identity()).limit(elements)
        })

    }

    override fun createFailedPublisher(): Publisher<Int> {
        return StreamPublisher<Int>(Supplier {
            throw RuntimeException()
        })
    }


}