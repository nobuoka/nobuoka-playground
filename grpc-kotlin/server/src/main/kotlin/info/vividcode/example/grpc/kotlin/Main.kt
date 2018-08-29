package info.vividcode.example.grpc.kotlin

import io.grpc.ServerBuilder
import io.grpc.examples.helloworld.GreeterGrpc

fun main(args: Array<String>) {
    ServerBuilder.forPort(8080)
            .addService(object : GreeterGrpc.GreeterImplBase() {

            })
}
