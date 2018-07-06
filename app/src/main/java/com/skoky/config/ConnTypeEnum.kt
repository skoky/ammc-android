package com.skoky.config

enum class ConnTypeEnum private constructor(private val label: String) {
    TCPIP("TCP/IP"),
    TCPIPAUTO("TCP/IP autosearch");

    override fun toString(): String {
        return super.toString()
    }

    companion object {

        val asItemsArray: Array<CharSequence?>
            get() {

                val array = arrayOfNulls<CharSequence>(values().size)
                for (i in 0 until values().size) {
                    array[i] = values()[i].name
                }
                return array
            }
    }
}
