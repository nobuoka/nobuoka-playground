package info.vividcode.sample.grpc

import android.os.Bundle
import android.support.design.widget.Snackbar
import android.support.v7.app.AppCompatActivity
import android.view.View

import kotlinx.android.synthetic.main.activity_main.*
import io.grpc.examples.helloworld.HelloReply
import io.grpc.examples.helloworld.HelloRequest
import io.grpc.examples.helloworld.GreeterGrpc
import io.grpc.ManagedChannelBuilder
import io.grpc.ManagedChannel



class MainActivity : AppCompatActivity() {
    val channel = ManagedChannelBuilder.forAddress("10.0.2.2", 6565)
            .usePlaintext(true)
            .build()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        setSupportActionBar(toolbar)

        fab.setOnClickListener { view ->
            Snackbar.make(view, "Replace with your own action", Snackbar.LENGTH_LONG)
                    .setAction("Action", null).show()
        }

        findViewById<View>(android.R.id.content).setOnClickListener {
            Thread(Runnable {
                val stub = GreeterGrpc.newBlockingStub(channel)
                val request = HelloRequest.newBuilder()
                        .setName("Tom")
                        .build()
                val reply = stub.sayHello(request)
                println("Reply: $reply")
            }).start()
        }
    }

}
