/**
 * see: https://stackoverflow.com/a/16049086/1251543
 */
@TypeDefs( {@TypeDef( name= StringJsonUserType.JSONB_TYPE, typeClass = StringJsonUserType.class)})

package org.cobbzilla.wizard.model.json;

import org.hibernate.annotations.TypeDef;
import org.hibernate.annotations.TypeDefs;