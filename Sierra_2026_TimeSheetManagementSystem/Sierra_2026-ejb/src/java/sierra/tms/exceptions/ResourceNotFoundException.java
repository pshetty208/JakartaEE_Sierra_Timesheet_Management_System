package sierra.tms.exceptions;

public class ResourceNotFoundException extends TmsExceptionHandler {

    public ResourceNotFoundException(String resource, Object id) {
        super("RESOURCE_NOT_FOUND", "error.resourceNotFound", resource, id);
    }
}