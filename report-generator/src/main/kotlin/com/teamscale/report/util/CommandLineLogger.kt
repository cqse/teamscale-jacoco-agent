package com.teamscale.report.util

/** Logger that prints all output to the console.  */
class CommandLineLogger : ILogger {
	override fun debug(message: String) {
		println(message)
	}

	override fun info(message: String) {
		println(message)
	}

	override fun warn(message: String) {
		System.err.println(message)
	}

	override fun warn(message: String, throwable: Throwable?) {
		System.err.println(message)
		throwable?.printStackTrace()
	}

	override fun error(throwable: Throwable) {
		throwable.printStackTrace()
	}

	override fun error(message: String, throwable: Throwable?) {
		System.err.println(message)
		throwable?.printStackTrace()
	}
}
