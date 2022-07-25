@jakarta.xml.bind.annotation.adapters.XmlJavaTypeAdapters({
		@jakarta.xml.bind.annotation.adapters.XmlJavaTypeAdapter(type = mobac.program.model.TileImageType.class, value = mobac.program.jaxb.TileImageTypeAdapter.class),
		@jakarta.xml.bind.annotation.adapters.XmlJavaTypeAdapter(type = java.awt.Color.class, value = mobac.program.jaxb.ColorAdapter.class),
		@jakarta.xml.bind.annotation.adapters.XmlJavaTypeAdapter(type = CoordinateUnit.class, value = mobac.program.jaxb.CoordinateUnitAdapter.class)

})
package mobac.mapsources.custom;

import mobac.mapsources.custom.CustomWmsMapSource.CoordinateUnit;
