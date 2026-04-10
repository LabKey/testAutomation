function managedColumns() {
    return {
        insert: ["Country"],
        update: ["Country"],
    };
}
exports.managedColumns = managedColumns;

function afterInsert(row, errors) {
    if (row.comments === 'AfterInsert') {
        errors[null] = 'This is the After Insert Error';
    }
}
exports.afterInsert = afterInsert;

function afterUpdate(row, oldRow, errors) {
    if (row.Comments === 'AfterUpdate') {
        errors[null] = 'This is the After Update Error';
    }
}
exports.afterUpdate = afterUpdate;

function afterDelete(row, errors) {
    if (row.country === 'Before Update changed me') {
        errors[null] = 'This is the After Delete Error';
    }
}
exports.afterDelete = afterDelete;

function beforeInsert(row, errors) {
    if (row.name === 'Managed Insert' || row.comments === 'Managed Insert') {
        row.Country = 'MANAGED-INS';
    } else if (row.name === 'Managed Struct' || row.comments === 'Managed Struct') {
        row.Country = 'MANAGED-STRUCT';
        row.undeclaredInsertCol = 'bad';
    } else if (row.name === 'Managed Struct Remove' || row.comments === 'Managed Struct Remove') {
        row.Country = 'MANAGED-STRUCT-REM';
        delete row.Comments;
    } else if (row.name === 'Managed Unhandled' || row.comments === 'Managed Unhandled') {
        delete row.Country;
    } else {
        if (row.Comments) {
            if (row.Comments === 'Managed Merge') {
                row.Country = 'MANAGED-MERGE';
            } else if (row.Comments === 'Individual Test') {
                row.Country = 'Inserting Single';
                row.Comments = 'BeforeDelete';
            } else if (row.Comments === 'Import Test') {
                row.Country = 'Importing TSV';
            } else if (row.Comments === 'API Test') {
                row.Country = 'API BeforeInsert';
            }
        }
        if (!row.Country) {
            row.Country = 'COUNTRY-DEFAULT';
        }
    }
}
exports.beforeInsert = beforeInsert;

function beforeUpdate(row, oldRow, errors) {
    if (row.Comments === 'Managed Update') {
        row.Country = 'MANAGED-UPD';
    } else if (row.Comments === 'Managed Struct') {
        row.Country = 'MANAGED-STRUCT';
        row.undeclaredUpdateCol = 'bad';
    } else if (row.Comments === 'Managed Struct Remove') {
        row.Country = 'MANAGED-STRUCT-REM';
        delete row.Comments;
    }

    if (row.Comments === 'Managed Unhandled') {
        delete row.Country;
    } else {
        if (row.Comments === 'BeforeUpdate')
            row.Country = 'Before Update changed me';
        else if (row.Comments === 'BeforeUpdate')
            row.country = 'Before Update changed me';
        if (!row.Country) {
            row.Country = 'COUNTRY-DEFAULT-UPD';
        }
    }
}
exports.beforeUpdate = beforeUpdate;

function beforeDelete(row, errors) {
    if (row.Comments === 'BeforeDelete') {
        errors[null] = 'This is the Before Delete Error';
    }
}
exports.beforeDelete = beforeDelete;
