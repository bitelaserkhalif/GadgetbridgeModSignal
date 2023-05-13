/*  Copyright (C) 2022 José Rebelo

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
import blk.freeyourgadget.gadgetbridge.entities.WorldClockDao;

public class GadgetbridgeUpdate_45 implements DBUpdateScript {
    @Override
    public void upgradeSchema(final SQLiteDatabase db) {
        if (!DBHelper.existsColumn(WorldClockDao.TABLENAME, WorldClockDao.Properties.Code.columnName, db)) {
            final String statement = "ALTER TABLE " + WorldClockDao.TABLENAME + " ADD COLUMN "
                    + WorldClockDao.Properties.Code.columnName + " TEXT";
            db.execSQL(statement);
        }

        if (!DBHelper.existsColumn(WorldClockDao.TABLENAME, WorldClockDao.Properties.Enabled.columnName, db)) {
            final String statement = "ALTER TABLE " + WorldClockDao.TABLENAME + " ADD COLUMN "
                    + WorldClockDao.Properties.Enabled.columnName + " BOOLEAN DEFAULT TRUE";
            db.execSQL(statement);
        }
    }

    @Override
    public void downgradeSchema(final SQLiteDatabase db) {
    }
}
