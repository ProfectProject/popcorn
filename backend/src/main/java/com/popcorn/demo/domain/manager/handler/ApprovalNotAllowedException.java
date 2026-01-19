package com.popcorn.demo.domain.manager.handler;

public class ApprovalNotAllowedException extends RuntimeException{

    public ApprovalNotAllowedException(String message) {super(message);}
}
