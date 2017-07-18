/*
 * Created by akshay.ahuja on 18/07/17.
 */

import com.jayway.restassured.RestAssured;
import com.jayway.restassured.response.Response;
import org.testng.Assert;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;
import org.testng.asserts.SoftAssert;
import org.yaml.snakeyaml.Yaml;

import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;
import javax.script.ScriptException;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.util.*;

import static com.jayway.restassured.RestAssured.given;

/**
 * Created by akshay.ahuja on 10/07/17.
 */
public class DataValidation {
    private SoftAssert softAssert = new SoftAssert();


    @DataProvider(name = "DataValidation")
    public Object[][] testcasegenerator() {

        InputStream reader = null;
        try {
            reader = new FileInputStream(new File(
                    "src/main/resources/data.yaml"));
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }

        Yaml yaml = new Yaml();
        List<Object[]> testData = Collections.synchronizedList(new ArrayList<>());

        for (Object value : yaml.loadAll(reader)) {

            Map<String, Object> element = (Map) value;
            testData.add(new Object[]{
                    element.get("testcase").toString(),
                    element.get("json").toString(),
                    element.get("expectedStatus").toString(),
                    element.get("input"),
                    element.get("validate")
            });

        }
        return testData.toArray(new Object[][]{});
    }


    @BeforeClass()
    public void setUp() {

        PropertyLoader propertyLoader = new PropertyLoader();
        Properties prop = propertyLoader.getProperty();
        String server = (String) prop.get("spring.server");
        Integer port = Integer.parseInt(prop.get("spring.middleware.port").toString());
        String hostname = System.getProperty("hostname");
        if (Objects.equals(hostname, null)) {
            RestAssured.baseURI = server;
        } else {
            RestAssured.baseURI = "http://" + hostname;
        }
        RestAssured.port = port;
        RestAssured.basePath = "programmatic/data/api/v2";

    }


    @Test(dataProvider = "DataValidation")
    public void valid(String testcase, String json, String expectedStatus, List inputs, String validate) throws ScriptException {
        System.out.println(inputs);
        System.out.println(validate);
        System.out.println(json);

        if (expectedStatus.equals("positive")) {
            Response responsedata = given()
                    .header("cache-control", "no-cache")
                    .header("postman-token", "a02379b9-3724-a7bb-1863-96bedf292e8a")
                    .header("userid", "kshitiz.lohia")
                    .header("Content-Type", "application/json")
                    .contentType("application/json")
                    .body(json).with()
                    .when()
                    .put("/resources/DynamicResource?from_ts=1493942400&to_ts=1495065600&granularityType=daily&role=94")
                    .then()
                    .statusCode(200)
                    .extract().response();
            String[] parts = validate.split("=");
            for (String part : parts) {
                System.out.println(part);
            }
            ScriptEngineManager mgr = new ScriptEngineManager();
            ScriptEngine engine = mgr.getEngineByName("JavaScript");
            for (Integer i = 0; i < 50; i++) {
                validate = validate.replace(inputs.get(0).toString(), responsedata.then().extract()
                        .path("data.resource_info[0].fields_data[" + i.toString() + "]." + inputs.get(0) + "").toString());
                validate = validate.replace(inputs.get(1).toString(), responsedata.then().extract()
                        .path("data.resource_info[0].fields_data[" + i.toString() + "]." + inputs.get(1) + "").toString());

                System.out.println(validate);
                Object clicks_check = responsedata.then().extract().path("data.resource_info[0].fields_data["
                        + i.toString() + "]." + inputs.get(0) + "");
                if ((Integer) clicks_check != 0) {
                    try {
                        System.out.println(engine.eval(validate));
                    } catch (ScriptException e) {
                        e.printStackTrace();
                    }

                    Float Delta = 0.1f;
                    Float asserting = responsedata.then().extract().path("data.resource_info[0].fields_data["
                            + i.toString() + "]." + parts[0] + "");
                    System.out.println(asserting);
                    softAssert.assertEquals(((Number) engine.eval(validate)).floatValue(), asserting, Delta);

                } else {
                    try {
                        System.out.println(engine.eval(validate));
                    } catch (ScriptException e) {
                        e.printStackTrace();
                    }
                    Float asserting = responsedata.then().extract().path("data.resource_info[0].fields_data[" +
                            i.toString() + "]." + parts[0] + "");
                    System.out.println(asserting);
                    Assert.assertEquals(0f, asserting);


                }
                validate = validate.replace(responsedata.then().extract().path("data.resource_info[0].fields_data["
                        + i.toString() + "]." + inputs.get(1) + "").toString(), inputs.get(1).toString());
                validate = validate.replace(responsedata.then().extract().path("data.resource_info[0].fields_data["
                        + i.toString() + "]." + inputs.get(0) + "").toString(), inputs.get(0).toString());

            }


        }

    }
}




