/*******************************************************************************
 * Copyright (c) MOBAC developers
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 2 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 ******************************************************************************/
package mobac.data.gpx;

import jakarta.xml.bind.JAXBContext;
import jakarta.xml.bind.JAXBException;
import jakarta.xml.bind.Marshaller;
import jakarta.xml.bind.Unmarshaller;
import jakarta.xml.bind.util.JAXBResult;
import mobac.data.gpx.gpx11.Gpx;
import mobac.utilities.Utilities;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Source;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import javax.xml.transform.stream.StreamSource;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.StringWriter;

public class GPXUtils {
	private static final Logger log = LoggerFactory.getLogger(GPXUtils.class);

	public static Gpx loadGpxFile(File f) throws JAXBException {
		// Create GPX 1.1 JAXB context
		JAXBContext context = JAXBContext.newInstance(Gpx.class);

		Unmarshaller unmarshaller = context.createUnmarshaller();
		try (InputStream is = new FileInputStream(f)) {
			DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
			factory.setNamespaceAware(true);
			DocumentBuilder loader = factory.newDocumentBuilder();
			Document document = loader.parse(is);
			if (log.isTraceEnabled()) {
				Transformer transformer = TransformerFactory.newInstance().newTransformer();
				transformer.setOutputProperty(OutputKeys.INDENT, "yes");
				transformer.setOutputProperty("{http://xml.apache.org/xslt}indent-amount", "2");
				StreamResult result = new StreamResult(new StringWriter());
				DOMSource source = new DOMSource(document);
				transformer.transform(source, result);
				String xmlString = result.getWriter().toString();
				log.trace("Loaded GPX document DOM:\n{}", xmlString);
			}
			String namespace = document.getDocumentElement().getNamespaceURI();
			if ("http://www.topografix.com/GPX/1/1".equals(namespace)) {
				return (Gpx) unmarshaller.unmarshal(document);
			}
			if ("http://www.topografix.com/GPX/1/0".equals(namespace)) {
				Source xmlSource = new javax.xml.transform.dom.DOMSource(document);
				Source xsltSource = new StreamSource(Utilities.loadResourceAsStream("xsl/gpx10to11.xsl"));
				JAXBResult result = new JAXBResult(unmarshaller);
				TransformerFactory transFact = TransformerFactory.newInstance();
				Transformer trans = transFact.newTransformer(xsltSource);
				trans.transform(xmlSource, result);
				return (Gpx) result.getResult();
			}
			throw new JAXBException("Expected GPX 1.0 or GPX1.1 namespace but found \n\"" + namespace + "\"");
		} catch (JAXBException e) {
			throw e;
		} catch (Exception e) {
			throw new JAXBException(e);
		}
	}

	public static void saveGpxFile(Gpx gpx, File f) throws JAXBException {
		// Create GPX 1.1 JAXB context
		JAXBContext context = JAXBContext.newInstance(Gpx.class);

		Marshaller marshaller = context.createMarshaller();
		marshaller.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, Boolean.TRUE);
		try (OutputStream os = new FileOutputStream(f)) {
			marshaller.marshal(gpx, os);
		} catch (IOException e) {
			throw new JAXBException(e);
		}
	}

	public static void main(String[] args) {
		try {
			loadGpxFile(new File("misc/samples/gpx/gpx11 wpt.gpx"));
			loadGpxFile(new File("misc/samples/gpx/gpx10 wpt.gpx"));
		} catch (JAXBException e) {
			e.printStackTrace();
		}
	}
}
