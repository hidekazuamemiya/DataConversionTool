package com.example.demo.exception;

public class ApplicationException extends RuntimeException {

	/**
	 * コンストラクタ
	 * @param message
	 */
	public ApplicationException(String message) {
		super(message);
	}

	/**
	 * コンストラクタ
	 * @param message
	 * @param cause
	 */
	public ApplicationException(String message, Throwable cause) {
		super(message, cause);
	}
}
