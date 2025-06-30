package model;

import java.util.ArrayList;
import java.util.List;

/**
 * Represents the model.RubyMethodMetadata class in the RubyTransformation project.
 *
 * @author Kashan Asim
 * @version 1.0
 * @project RubyTransformation
 * @module PACKAGE_NAME
 * @class model.RubyMethodMetadata
 * @lastModifiedBy Kashan.Asim
 * @lastModifiedDate 6/29/2025
 * @license Licensed under the Apache License, Version 2.0
 * @description A brief description of the class functionality.
 * @notes <ul>
 * <li>Provide any additional notes or remarks here.</li>
 * </ul>
 * @since 6/29/2025
 */
public class RubyMethodMetadata {
    public String southBoundVersion;
    public String methodName;
    public List<String> queryParams = new ArrayList<>();
    public String microService;
    public String operation;
    public String backendVersion;
    public String endpoint; // computed as {micro_service}/{backend_ver}/{operation}
    public String responseUnwrapMethod;
    public String httpMethod;

    @Override
    public String toString() {
        return """
            Southbound Version: %s
            Method Name: %s
            HTTP Method: %s
            Query Params: %s
            Router Endpoint: %s
            Response Mapping: %s
            """.formatted(southBoundVersion,methodName, httpMethod, queryParams, endpoint, responseUnwrapMethod);
    }
}

