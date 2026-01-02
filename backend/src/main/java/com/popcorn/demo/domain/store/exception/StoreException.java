package com.popcorn.demo.domain.store.exception;

import com.popcorn.demo.common.dto.ResponseCode;
import com.popcorn.demo.common.exception.BaseException;
import org.apache.coyote.Response;

public class StoreException extends BaseException {

    private StoreException(ResponseCode responseCode){

        super(responseCode);

    }

    private StoreException(ResponseCode responseCode, Throwable cause){

        super(responseCode, cause);

    }

    public static StoreException invalidRequest() {

        return new StoreException(ResponseCode.INVALID_REQUEST);

    }

    public static StoreException emptyName(){

        return new StoreException(ResponseCode.EMPTY_NAME);

    }

    public static StoreException ownerNotFound() {

        return new StoreException(ResponseCode.OWNER_NOT_FOUND);

    }



}
