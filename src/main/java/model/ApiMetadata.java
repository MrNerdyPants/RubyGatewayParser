package model;

import java.util.ArrayList;
import java.util.List;

/**
 * Represents the model.ApiMetadata class in the RubyTransformation project.
 *
 * @author Kashan Asim
 * @version 1.0
 * @project RubyTransformation
 * @module PACKAGE_NAME
 * @class model.ApiMetadata
 * @lastModifiedBy Kashan.Asim
 * @lastModifiedDate 6/29/2025
 * @license Licensed under the Apache License, Version 2.0
 * @description A brief description of the class functionality.
 * @notes <ul>
 * <li>Provide any additional notes or remarks here.</li>
 * </ul>
 * @since 6/29/2025
 */
public class ApiMetadata {
    public String northboundVersion;
    public String apiName;
    public List<String> headers = new ArrayList<>();
    public String httpMethod;
    public String endpoint;
    public String jsonBody;
    public String southboundVersion;
    public String southboundMethod;
}
