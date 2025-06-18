package functional_tests;

import io.eel.model.WorkbookOutput;
import io.eel.packager.WorkbookValidator;
import io.eel.service.WorkbookCalculationEngine;

public class FunctionalTests {

    private static final WorkbookCalculationEngine engine = new WorkbookCalculationEngine();

    public static void main(String[] args) {
        engine.withInput(
                        "input_customer",
                        new Object[][] {
                                {"id", "first_name", "middle_name", "last_name", "birth_day", "birth_month", "birth_year"},
                                {1, "joey", "", "jones", 12, 4, 1900},
                                {52, "dude", "my", "bro", 11, 2, 1999},
                                {62, "sam", "i", "am", 9, 5, 2010}
                        }
                )
                .withInput(
                        "input_customer_address",
                        new Object[][]{
                                {"id", "address_number", "address_street", "city", "state", "zip", "country_code"},
                                {1, 234, "Orchard", "Nova Scotia", "MN", 23242, "US"},
                                {52, 525, "Beach", "San Diego", "CA", 74354, "US"},
                                {62, 89, "Desert", "Austin", "TX", 80480, "US"}
                        }
                );

        WorkbookOutput output = engine.runWorkbook();
        output.logOutput();
    }

}
