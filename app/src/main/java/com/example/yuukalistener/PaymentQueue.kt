package com.example.yuukalistener

object PaymentQueue {
    private val queue = mutableListOf<Message>()

    @Synchronized
    fun enqueue(message: Message) {
        queue.add(message)
    }

    @Synchronized
    fun dequeue(): Message? {
        return if (queue.isNotEmpty()) queue.removeAt(0) else null
    }

    @Synchronized
    fun isNotEmpty(): Boolean = queue.isNotEmpty()
}
