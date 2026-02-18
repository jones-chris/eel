package io.eel.service;

import com.opencsv.CSVReader;
import io.eel.model.WorkbookOutput;

import java.io.InputStreamReader;

class WorkbookCalculationEngineTest {

    public static void main(String[] args) {
        WorkbookCalculationEngine engine = new WorkbookCalculationEngine();

        CSVReader inputCustomerReader = new CSVReader(new InputStreamReader(WorkbookCalculationEngineTest.class.getResourceAsStream("/input_customer.csv")));
        CSVReader inputCustomerAddressReader = new CSVReader(new InputStreamReader(WorkbookCalculationEngineTest.class.getResourceAsStream("/input_customer_address.csv")));

        engine.withInput("input_customer", inputCustomerReader);
        engine.withInput("input_customer_address", inputCustomerAddressReader);

        try {
            WorkbookOutput output = engine.runWorkbook();
            output.logOutput();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

}