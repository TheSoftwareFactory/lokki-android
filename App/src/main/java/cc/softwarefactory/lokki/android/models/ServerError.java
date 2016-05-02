package cc.softwarefactory.lokki.android.models;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Model Class for Server Errors
 */
public class ServerError {

    @JsonProperty("ErrorCode")
    private String errorCode;

    @JsonProperty("ErrorType")
    private String errorType;

    @JsonProperty("ErrorMessage")
    private String errorMessage;

    public String getErrorCode() { return errorCode; }

    public void setErrorCode( String errorCode ) { this.errorCode = errorCode; }

    public String getErrorType() { return errorType; }

    public void setErrorType( String errorType ) { this.errorType = errorType; }

    public String getErrorMessage() {return errorMessage; }

    public void setErrorMessage( String errorMessage ) { this.errorMessage = errorMessage; }
}
