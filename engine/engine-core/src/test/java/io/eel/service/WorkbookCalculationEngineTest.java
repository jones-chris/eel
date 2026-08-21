package io.eel.service;

import com.opencsv.CSVReader;
import io.eel.common.model.WorkbookOutput;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStreamReader;

class WorkbookCalculationEngineTest {

    public static void main(String[] args) throws FileNotFoundException {
        WorkbookCalculationEngine engine = new WorkbookCalculationEngine();

//        CSVReader inputCustomerReader = new CSVReader(new InputStreamReader(WorkbookCalculationEngineTest.class.getResourceAsStream("/input_customer.csv")));
//        CSVReader inputCustomerAddressReader = new CSVReader(new InputStreamReader(WorkbookCalculationEngineTest.class.getResourceAsStream("/input_customer_address.csv")));

        CSVReader inputCategories = new CSVReader(new InputStreamReader(new FileInputStream("/home/pc/Downloads/categories.csv")));
        CSVReader inputTransactions = new CSVReader(new InputStreamReader(new FileInputStream("/home/pc/Downloads/Simplifi_Transactions.csv")));

//        engine.withInput("input_customer", inputCustomerReader);
//        engine.withInput("input_customer_address", inputCustomerAddressReader);

        engine.withInput("input_categories", inputCategories);
        engine.withInput("input_transactions", inputTransactions);

        try {
            WorkbookOutput output = engine.runWorkbook();
            output.logOutput();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

}