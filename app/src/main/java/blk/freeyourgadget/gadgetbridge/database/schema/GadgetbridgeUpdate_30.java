/*  Copyright (C) 2017-2020 Andreas Shimokawa, protomors

    This file is part of Gadgetbridge.

    Gadgetbridge is free software: you can redistribute it and/or modify
    it under the terms of the GNU Affero General Public License as published
    by the Free Software Foundation, either version 3 of the License, or
    (at your option) any later version.

    Gadgetbridge is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU Affero General Public License for more details.

    You should have received a copy of the GNU Affero General Public License
    along with this program.  If not, see <http://www.gnu.org/licenses/>. */
package blk.freeyourgadget.gadgetbridge.database.schema;

import android.database.sqlite.SQLiteDatabase;

import blk.freeyourgadget.gadgetbridge.database.DBHelper;
import blk.freeyourgadget.gadgetbridge.database.DBUpdateScript;
import blk.freeyourgadget.gadgetbridge.entities.BaseActivitySummaryDao;

public class GadgetbridgeUpdate_30 implements DBUpdateScript {
    @Override
    public void upgradeSchema(SQLiteDatabase db) {
        if (!DBHelper.existsColumn(BaseActivitySummaryDao.TABLENAME, BaseActivitySummaryDao.Properties.RawSummaryData.columnName, db)) {
            String ADD_COLUMN_RAW_SUMMARY_DATA = "ALTER TABLE " + BaseActivitySummaryDao.TABLENAME + " ADD COLUMN "
                    + BaseActivitySummaryDao.Properties.RawSummaryData.columnName + " BLOB";
            db.execSQL(ADD_COLUMN_RAW_SUMMARY_DATA);
        }
    }

    @Override
    public void downgradeSchema(SQLiteDatabase db) {
    }
}
