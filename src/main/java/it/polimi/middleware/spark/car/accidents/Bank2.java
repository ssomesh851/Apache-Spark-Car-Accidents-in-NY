package it.polimi.middleware.spark.car.accidents;

import static org.apache.spark.sql.functions.max;

import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

import org.apache.spark.sql.Dataset;
import org.apache.spark.sql.Row;
import org.apache.spark.sql.SparkSession;
import org.apache.spark.sql.types.DataTypes;
import org.apache.spark.sql.types.StructField;
import org.apache.spark.sql.types.StructType;

import it.polimi.middleware.spark.tutorial.utils.LogUtils;

/**
 * Bank example
 *
 * Input: csv files with list of deposits and withdrawals, having the following
 * schema ("person: String, account: String, amount: Int)
 *
 * Two queries Q1. Print the person with the maximum total amount of withdrawals
 * Q2. Print all the accounts with a negative balance
 *
 * The code exemplifies the use of SQL primitives
 */
public class Bank2 {
	public static void main(String[] args) {
		LogUtils.setLogLevel();

		final String master = args.length > 0 ? args[0] : "local[4]";
		final String filePath = args.length > 1 ? args[1] : "./";

		final SparkSession spark = SparkSession //
		    .builder() //
		    .master(master) //
		    .appName("Bank") //
		    .getOrCreate();

		final List<StructField> mySchemaFields = new ArrayList<>();
		mySchemaFields.add(DataTypes.createStructField("person", DataTypes.StringType, true));
		mySchemaFields.add(DataTypes.createStructField("account", DataTypes.StringType, true));
		mySchemaFields.add(DataTypes.createStructField("amount", DataTypes.IntegerType, true));
		final StructType mySchema = DataTypes.createStructType(mySchemaFields);

		final Dataset<Row> deposits = spark //
		    .read() //
		    .option("header", "false") //
		    .option("delimiter", ",") //
		    .schema(mySchema) //
		    .csv(filePath + "files/bank/deposits.csv");

		final Dataset<Row> withdrawals = spark //
		    .read() //
		    .option("header", "false") //
		    .option("delimiter", ",") //
		    .schema(mySchema) //
		    .csv(filePath + "files/bank/withdrawals.csv");

		// Used in two different queries
		withdrawals.cache();

		// Q1 Person with the maximum total amount of withdrawals

		final Dataset<Row> sumWithdrawals = withdrawals //
		    .groupBy("person") //
		    .sum("amount") //
		    .select("person", "sum(amount)");

		final long maxTotal = sumWithdrawals //
		    .agg(max("sum(amount)")) //
		    .first() //
		    .getLong(0);

		final Dataset<Row> maxWithdrawals = sumWithdrawals //
		    .filter(sumWithdrawals.col("sum(amount)").equalTo(maxTotal));

		maxWithdrawals.show();

		// Q2 Accounts with negative balance

		final Dataset<Row> totWithdrawals = withdrawals //
		    .groupBy("account") //
		    .sum("amount") //
		    .drop("person") //
		    .as("totalWithdrawals");

		final Dataset<Row> totDeposits = deposits //
		    .groupBy("account") //
		    .sum("amount") //
		    .drop("person") //
		    .as("totalDeposits");

		final Dataset<Row> negativeAccounts = totWithdrawals //
		    .join(totDeposits, totDeposits.col("account").equalTo(totWithdrawals.col("account")), "left_outer") //
		    .filter(//
		        totDeposits.col("sum(amount)").isNull().and(totWithdrawals.col("sum(amount)").gt(0)).or //
						(totWithdrawals.col("sum(amount)").gt(totDeposits.col("sum(amount)"))) //
				).drop("sum(amount)");

		negativeAccounts.show();

		Scanner a = new Scanner(System.in);
		a.nextLine();
		spark.close();

	}
}