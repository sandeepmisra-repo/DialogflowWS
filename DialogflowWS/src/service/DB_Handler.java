package service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import CrsCde.CODE.SQLite.DB.SQLiteDBManager;
import CrsCde.CODE.SQLite.DB.SQLiteOneDBManager;

public class DB_Handler {
	public Logger logger = LoggerFactory.getLogger(DB_Handler.class);
	SQLiteDBManager _dbMgr;

	public DB_Handler() {
		_dbMgr = SQLiteOneDBManager.This();
	}

	public void DB_Insert(String BUnit) {
		try {
			String data = "Insert into `AccessToken` (\n" + "`BUnit`)\n" + " values(\n" + "" + BUnit + ");";

			_dbMgr.Execute(data);
			logger.info("Data Insert Successfully ");
		} catch (Exception e) {
			logger.error(e.getMessage(), e);
		}
	}

	public void DB_Create() {
		try {
			String dropTbl = "drop table `AccessToken`";
			_dbMgr.Execute(dropTbl);
			logger.info("Database Drop Successfully");
		} catch (Exception e) {
			logger.error(e.getMessage(), e);
		}
		try {
			String ClientTable = "CREATE TABLE `AccessToken` (\n"
					+ "	`Id`	INTEGER NOT NULL PRIMARY KEY AUTOINCREMENT,\n" + "	`BUnit`	TEXT  NULL ,\n"
					+ "	`Token`	TEXT  NULL\n" + ");";

			_dbMgr.Execute(ClientTable);
			logger.info("Table Create Successfully ");

		} catch (Exception e) {
			logger.error(e.getMessage(), e);
		}

	}
}
