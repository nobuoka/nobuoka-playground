package info.vividcode.example.grpc

import io.grpc.ServerBuilder
import io.grpc.examples.helloworld.GreeterGrpc
import io.grpc.examples.helloworld.HelloReply
import io.grpc.examples.helloworld.HelloRequest
import io.grpc.stub.StreamObserver

fun main(args: Array<String>) {

    val server = ServerBuilder
            .forPort(6565)
            .addService(object : GreeterGrpc.GreeterImplBase() {
                override fun sayHello(request: HelloRequest?, responseObserver: StreamObserver<HelloReply>?) {
                    val reply = HelloReply.newBuilder().setMessage("Hello " + request?.name).build()
                    responseObserver?.onNext(reply)
                    responseObserver?.onCompleted()
                }
            })
            .build()

    server.start()

    server.awaitTermination()
}
