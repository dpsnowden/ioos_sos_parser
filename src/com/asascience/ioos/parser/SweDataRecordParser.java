package com.asascience.ioos.parser;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.jdom2.Document;
import org.jdom2.Element;
import org.jdom2.JDOMException;
import org.jdom2.Namespace;
import org.jdom2.input.SAXBuilder;

import com.asascience.ioos.model.Coordinate;
import com.asascience.ioos.model.MemberObservation;
import com.asascience.ioos.model.QualityModel;
import com.asascience.ioos.model.SensorDataRecords;
import com.asascience.ioos.model.SensorProperty;
import com.asascience.ioos.model.SensorModel;
import com.asascience.ioos.model.SensorProperty.PropertyType;
import com.asascience.ioos.model.SweDataRecord;
import com.asascience.ioos.model.VectorModel;


public class SweDataRecordParser {
	public enum SweFileType{
		TIME_SERIES("timeSeries"),
		TIME_SERIES_PROFILE("timeSeriesProfile");
		private final String typeName;
		SweFileType(String typeName){
			this.typeName = typeName;
		}
	}
	private SweFileType sweFileType;
	private Namespace swe2Ns;
	private Namespace xsiNs;
	private Namespace xlinkNs;
	private String decimalSeparator = ".";
	private String tokenSeparator = ",";
	private String blockSeparator = "\n";
	private final String hrefTag = "href";
	private final String swe2NsTag = "swe2";
	private final String idTag = "id";
	private final String xlinkNsTag = "xlink";
	private final String xsiNsTag = "xsi";
	private final String fieldTag = "field";
	private final String textTag = "Text";
	private final String attNameTag = "name";
	private final String axisIdTag = "axisID";
	private final String itemTag = "item";
	private final String uomTag = "uom";
	private final String labelTag = "label";
	private final String valueTag = "value";
	private final String dynamicValuesTag = "values";
	private final String nilValuesTag = "nilValues";
	private final String nilValueTag = "nilValue";
	private final String codeTag = "code";
	private final String codeSpaceTag = "codeSpace";
	private final String attDefinitionTag = "definition";
	private final String referenceFrameTag = "referenceFrame";
	private final String dataRecordTag = "DataRecord";
	private final String dataArrayTag = "DataArray";
	private final String elementCountTag  = "elementCount";
	private final String elementTypeTag = "elementType";
	private final String dataChoiceTag = "DataChoice";
	private final String encodingTag = "encoding";
	private final String textEncodingTag = "TextEncoding";
	private final String decimalSeparatorAtt = "decimalSeparator";
	private final String tokenSeparatorAtt = "tokenSeparator";
	private final String blockSeparatorAtt = "blockSeparator";
	private final String countTag = "Count";
	private final String qualityTag = "quality";
	private final String quantityTag = "Quantity";
	private final String quantityRangeTag = "QuantityRange";

	private final String constraintTag =  "constraint";
	private final String allowedTokenTag = "AllowedTokens";
	private final String descriptionTag = "description";
	private final String timeTag = "Time";
	private final String stationsStaticDataDef = "http://mmisw.org/ont/ioos/definition/stations";
	private final String stationShortIdDefinition = "http://mmisw.org/ont/ioos/definition/station";
	private final String stationIdDefinition ="http://mmisw.org/ont/ioos/definition/stationID";
	private final String latitudeCoordDefinition = "http://mmisw.org/ont/cf/parameter/latitude";
	private final String longitudeCoordDefinition = "http://mmisw.org/ont/cf/parameter/longitude";
	private final String zCoodDefinition = "http://mmisw.org/ont/cf/parameter/height";
	private final String sensorObsCollectionDynDataDef = "http://mmisw.org/ont/ioos/definition/sensorObservationCollection";
	private final String sensorObservationDefiniton = "http://mmisw.org/ont/ioos/definition/sensorObservations";
	private final String sensorDefinition = "http://mmisw.org/ont/ioos/definition/sensor";
	private final String sensorListDefintion = "http://mmisw.org/ont/ioos/definition/sensors";
	private final String sensorIdDefintion = "http://mmisw.org/ont/ioos/definition/sensorID";
	private final String sensorOrientationDefinition = "http://www.opengis.net/def/property/OGC/0/SensorOrientation";
	private final String sensorLocationDefinition = "http://www.opengis.net/def/property/OGC/0/SensorLocation";
	private final String samplingTimeDefinition = "http://www.opengis.net/def/property/OGC/0/SamplingTime";
	private final String profileHeightDefinition = "http://mmisw.org/ont/ioos/definition/profileHeights";
	private final String platformLocationDef ="http://www.opengis.net/def/property/OGC/0/PlatformLocation";
	private final String heightDefinition="http://mmisw.org/ont/cf/parameter/height";
	private final String profileBinsDefinition="http://mmisw.org/ont/ioos/definition/profileBins";
	public SweDataRecordParser(String sweFileTypeStr){
		for(SweFileType type : SweFileType.values()){
			if (type.typeName.equals(sweFileTypeStr)){
				this.sweFileType = type;
				break;
			}
		}
	}
	
	public void parseSweDataRecord(String xmlFileName, MemberObservation memberObs) 
			throws JDOMException, IOException{
		File xmlFile = new File(xmlFileName);
		if(xmlFile.exists()){
			Document xmlDoc = new SAXBuilder().build(xmlFile);
			Element root = xmlDoc.getRootElement();
			parseSweDataRecord(root, memberObs);
		}
		return;
	}
	
	
	private void initSweNamespaces(Element root){
		for(Namespace ns : root.getAdditionalNamespaces()){
			switch(ns.getPrefix()){
			case swe2NsTag:
				swe2Ns = ns;
				break;
			case xsiNsTag:
				xsiNs = ns;
				break;
			case xlinkNsTag:
				xlinkNs = ns;
				break;
			}
		}	
	}
	

	
	
	
	public void parseSweDataRecord(Element sweDataRecord, MemberObservation memberObs){
		if(sweDataRecord == null) return;
		SweDataRecord sweRecord = new SweDataRecord();
		initSweNamespaces(sweDataRecord);
		List<Element> fieldNameElem = sweDataRecord.getChildren(fieldTag, swe2Ns);
		boolean isStatic;
		for(Element elem : fieldNameElem){
			isStatic = determineIfStatic(elem);
			if(isStatic)
				parseStaticData(elem, sweRecord);
			else
				parseDynamicData(elem, sweRecord);
			
		}
		
		
		
		
		memberObs.setSweDataRecords(sweRecord);
		
		return;
	}
	
	private boolean determineIfStatic(Element fieldRecord){
		boolean isStatic = false;
		if(fieldRecord != null){
			Element dataRecord = fieldRecord.getChild(dataRecordTag, swe2Ns);
			if(dataRecord != null){
			String definitionStr = dataRecord.getAttributeValue(attDefinitionTag);
			if(definitionStr.equals(stationsStaticDataDef))
				isStatic = true;
			}
		}
		return isStatic;
	}
	
	// parse the station record
	private void parseStationRecord(Element stationDataRecord, SweDataRecord sweRecord ){
		
		if(stationDataRecord != null){
			String dataRecordId = null;
			if(stationShortIdDefinition.equals(stationDataRecord.getAttributeValue(attDefinitionTag)))
				dataRecordId = stationDataRecord.getAttributeValue(idTag);
			String elemDef;
			VectorModel stationPlatformLocation = null;
			
			List<SensorModel> sensorModelList = null;
			for(Element stationField : stationDataRecord.getChildren(fieldTag, swe2Ns)) {

				for(Element childPropertyField : stationField.getChildren()) {
					elemDef = childPropertyField.getAttributeValue(attDefinitionTag);
					// station id record
					if(elemDef.equals(stationIdDefinition)) {
						Element stationTextElem = stationField.getChild(textTag, swe2Ns);
						// process the station definitions
						if(stationTextElem != null){
							String stationFullId = stationTextElem.getChildText(valueTag, swe2Ns);
							sweRecord.addStationIdMap(dataRecordId, stationFullId);
							List<QualityModel> qualityList = parseQualityRecord(stationTextElem);
							if(qualityList != null)
								sweRecord.addQualityMapEntry(stationFullId, qualityList);
						}
					}
					else if(elemDef.equals(platformLocationDef)){
						stationPlatformLocation = parseVectorLocationRecord(childPropertyField);
					}
					else if(elemDef.equals(this.sensorListDefintion)){
						sensorModelList = parseStaticSensorRecord(childPropertyField, sweRecord);
					}
				
				}
			}
			sweRecord.addPlatformLocationData(dataRecordId, stationPlatformLocation);
			sweRecord.addSensorIdtoSensorDataMapEntry(dataRecordId, sensorModelList);
		}
	}
	
	// parses the sensor block in the static data
	private List<SensorModel> parseStaticSensorRecord(Element sensorDef, SweDataRecord sweRecord){
		List<SensorModel> sensorList = new ArrayList<SensorModel>();
		for(Element sensorField : sensorDef.getChildren()){
			for(Element sensorRecord : sensorField.getChildren()){
				String sensorDefStr = sensorRecord.getAttributeValue(attDefinitionTag);
				if(sensorDefinition.equals(sensorDefStr)){
					SensorModel sensorModel = new SensorModel();

					for(Element sensorFields : sensorRecord.getChildren()){
						for(Element sensorFieldType : sensorFields.getChildren()){
							String sensorFieldDefStr = sensorFieldType.getAttributeValue(attDefinitionTag);
							List<QualityModel> qualModel = this.parseQualityRecord(sensorFieldType);
							// parse the sensor height field
							if(heightDefinition.equals(sensorFieldDefStr)){
								Coordinate zCoord = new Coordinate();
								zCoord.setAxisId(sensorFieldType.getAttributeValue(axisIdTag));
								zCoord.setReferenceFrame(sensorFieldType.getAttributeValue(referenceFrameTag));
								Element childElem = sensorFieldType.getChild(uomTag, swe2Ns);
								if(childElem != null)
									zCoord.setUnitOfMeasure(childElem.getAttributeValue(codeTag));

								childElem = sensorFieldType.getChild(valueTag, swe2Ns);
								if(childElem != null) {
									try{
										zCoord.setCoordinateValue(Double.valueOf(childElem.getText()));
									}
									catch(NumberFormatException e){
										e.printStackTrace();
									}
								}
								sensorModel.setSensorHeight(zCoord);
							}
							else if(this.sensorLocationDefinition.equals(sensorFieldDefStr)){
								// parse the sensor location
								VectorModel sensorLoc = this.parseVectorLocationRecord(sensorFieldType);
								sensorModel.setSensorLocation(sensorLoc);
								
							}
							else if(this.sensorOrientationDefinition.equals(sensorFieldDefStr)){
								// parse the sensor orientation field
							}
							else if(profileBinsDefinition.equals(sensorFieldDefStr) ||
									profileHeightDefinition.equals(sensorFieldDefStr)){
								parseProfileBins(sensorFieldType,  
										sensorFieldType.getAttributeValue(attNameTag), 
										sensorModel);
							}
							// parse the sensor ID field
							else if(this.sensorIdDefintion.equals(sensorFieldDefStr)){
								String sensorID = sensorFieldType.getChildText(valueTag, swe2Ns);
								sensorModel.setSensorId(sensorID);
								if(qualModel != null)
									sensorModel.setSensorQualityModel(qualModel);
								sweRecord.addSensorIdMap(sensorID, sensorRecord.getAttributeValue(idTag));
							}
						}
					}
					sensorList.add(sensorModel);

				}
				
			}
			
			
		}
		return sensorList;
	}
	
	
	private void parseProfileBins(Element dataArray, String profileId, SensorModel sensorModel){
		if(dataArray != null){
			List<SensorProperty> prop = processDataArrayRecord(dataArray);
			SensorDataRecords sensorData = new SensorDataRecords(profileId, prop);


			parseEncodingElement(dataArray);

			// parse the bin data values
			Element valueElement = dataArray.getChild(dynamicValuesTag, swe2Ns);
			 parseValuesForSensorRecord(sensorData, valueElement);
				sensorModel.setBinDefRecord(sensorData);
			
	
			//parse the element type
			
		}
	}
	
	
	// Parses the platform location record in the static data block
	private VectorModel parseVectorLocationRecord(Element platformDef){
		VectorModel vectorMod = new VectorModel();
		if(platformDef != null){
			String coordDef;
			Coordinate latCoord = null;
			Coordinate lonCoord = null;
			Coordinate zCoord = null;
			// get the coordinates
			for(Element coord : platformDef.getChildren()){
				for(Element coordType : coord.getChildren()){
					coordDef = coordType.getAttributeValue(attDefinitionTag);
					Coordinate tempCoord = new Coordinate();
					tempCoord.setAxisId(coordType.getAttributeValue(axisIdTag));
					tempCoord.setCoordinateName(coord.getAttributeValue(attNameTag));
					Element childElem = coordType.getChild(uomTag, swe2Ns);
					if(childElem != null)
						tempCoord.setUnitOfMeasure(childElem.getAttributeValue(codeTag));
					childElem = coordType.getChild(valueTag, swe2Ns);
					if(childElem != null){
						try	{
							tempCoord.setCoordinateValue(Double.valueOf(childElem.getText()));
						}
						catch(NumberFormatException e){
							tempCoord.setCoordinateValue(null);
							e.printStackTrace();
						}
						switch(coordDef){


						case latitudeCoordDefinition:
							latCoord = tempCoord;

							break;
						case longitudeCoordDefinition:
							lonCoord = tempCoord;
							break;
						case zCoodDefinition:
							zCoord = tempCoord;
							break;
						}


					}



				}

			}
			vectorMod.setCoordinates(latCoord, lonCoord, zCoord);
		}
		return vectorMod;
	}
	
	
	private void parseStaticData(Element staticElem, SweDataRecord sweRecord ){
		Element dataRecord = staticElem.getChild(dataRecordTag, swe2Ns);
		String childRecDef;
		// Get the static data for each station
		for(Element fieldStationId :  dataRecord.getChildren(fieldTag, swe2Ns)){
			 childRecDef = fieldStationId.getChild(dataRecordTag, swe2Ns).getAttributeValue(attDefinitionTag);
			 if(childRecDef.equals(stationShortIdDefinition))
				 parseStationRecord( fieldStationId.getChild(dataRecordTag, swe2Ns), sweRecord);
			
		}
	}
	
	
	private void parseEncodingElement(Element dataArrayElem){
		if(dataArrayElem !=  null) {
			// parse the encodings
			Element encodingElem = dataArrayElem.getChild(encodingTag, swe2Ns);
			if(encodingElem != null){
				Element textEncodingElem = encodingElem.getChild(textEncodingTag, swe2Ns);
				decimalSeparator = textEncodingElem.getAttributeValue(decimalSeparatorAtt);
				tokenSeparator = textEncodingElem.getAttributeValue(tokenSeparatorAtt);
				blockSeparator = textEncodingElem.getAttributeValue(blockSeparatorAtt);

			}
		}
	}

	private void parseDynamicData(Element dynamicElem, SweDataRecord sweRecord){
		if(dynamicElem != null){
			String decimalSeparator = ".";
			String tokenSeparator = ",";
			String blockSeparator = "\n";
			Integer dataRows = 0;
			Element dataArrayElem = dynamicElem.getChild(dataArrayTag, swe2Ns);
			if(dataArrayElem != null &&
			   sensorObsCollectionDynDataDef.equals(dataArrayElem.getAttributeValue(attDefinitionTag))){
				// parse the dynamic data for each sensor
				
				// determine the number of rows in the data
				List<QualityModel> elemQualRecords = parseElementCount(dataArrayElem.getChild(elementCountTag, swe2Ns), 
						sweRecord);
				sweRecord.setNumberDataRows(dataRows);
				
				// parse the observations record
				parseElementTypeRecord(dataArrayElem.getChild(elementTypeTag, swe2Ns), sweRecord);
				

				// add the quality records to the modeled properties
				if(elemQualRecords != null){
					for(QualityModel quality : elemQualRecords){
						for( SensorModel sensModel  : sweRecord.getSensorIdtoSensorDataMap().values()){
								SensorDataRecords sensorRec = sensModel.getSensorDataRecord();
								if(sensorRec != null){
									SensorProperty prop = new SensorProperty();
									prop.setSensorType(elementCountTag);
									prop.setPropertyType(PropertyType.QUALITY);
									prop.setQualityMeasure(quality);
									sensorRec.getModeledProperties().add(0, prop);
								}
							
						}
					}
					sweRecord.setSectorStartColumn(sweRecord.getSectorStartColumn() + elemQualRecords.size());
					sweRecord.setTimeColumn(sweRecord.getTimeColumn() + elemQualRecords.size());
				}

				//parse the encodings
				parseEncodingElement(dataArrayElem);
				
				// parse the actual data values
				Element valueElement = dataArrayElem.getChild(dynamicValuesTag, swe2Ns);
				parseDynamicSensorDataValues(valueElement, sweRecord, decimalSeparator,
											 tokenSeparator, blockSeparator, this.sweFileType);
				
				// Remove count & index properties from the list as they
				// are not included in the result object
					for(SensorModel sensModel: sweRecord.getSensorIdtoSensorDataMap().values()){
						SensorDataRecords dataRec = sensModel.getSensorDataRecord();
					List<SensorProperty> toRemove = new ArrayList<SensorProperty>();
					for(SensorProperty prop : dataRec.getModeledProperties())
						if(prop.getPropertyType() == PropertyType.COUNT ||
						   prop.getPropertyType() == PropertyType.INDEX)
							toRemove.add(prop);
					dataRec.getModeledProperties().removeAll(toRemove);
				
				}		
			}
		}
	}
	
	private void parseElementTypeRecord(Element observationsRecord, SweDataRecord sweRecord){
	
		if(observationsRecord != null){
			Element dataRecord = observationsRecord.getChild(dataRecordTag, swe2Ns);
			String timeUomRef = null;
			Integer timeDataIndex = null;
			Integer sensorDataIndex = null;
			if(dataRecord != null &&  (sensorObservationDefiniton.equals(dataRecord.getAttributeValue(attDefinitionTag)))) {
				int currIndex = 0;
				for(Element fieldRecord : dataRecord.getChildren(fieldTag, swe2Ns)){
					Element childField;
					//parse the time record for the sensors;
					if((childField = fieldRecord.getChild(timeTag, swe2Ns)) != null){
						if(samplingTimeDefinition.equals(childField.getAttributeValue(attDefinitionTag))){
							Element uom = childField.getChild(uomTag, swe2Ns);
							if(uom != null)
								timeUomRef = uom.getAttributeValue(hrefTag, xlinkNs);
							
						}
						timeDataIndex = currIndex;
					}
					
					else if((childField = fieldRecord.getChild(dataChoiceTag, swe2Ns)) != null){
						//process the dynamic sensor data
						processDynamicSensorData(childField, sweRecord);
						sensorDataIndex = currIndex;
					}
					
					currIndex++;
				}
	
				
			}
			if(timeDataIndex != null){
					for(SensorModel sensModel: sweRecord.getSensorIdtoSensorDataMap().values()){
						SensorDataRecords dataRec = sensModel.getSensorDataRecord();
						SensorProperty timeProp = new SensorProperty();
						timeProp.setSensorType(samplingTimeDefinition);
						timeProp.setPropertyType(PropertyType.TIME);
						timeProp.setSensorUnitOfMeasure(timeUomRef);
						dataRec.addSensorPropertyAtIndex(timeProp, timeDataIndex);
					}
				
			}
			sweRecord.setTimeColumn(timeDataIndex);
			sweRecord.setSectorStartColumn(sensorDataIndex);
			
		}
	}
	

	private void parseValuesForSensorRecord(SensorDataRecords sensorRec, Element valuesElem){
		if(valuesElem != null && sensorRec != null){

			String valueBlock = valuesElem.getValue();
			String[] rowBlock = valueBlock.split(blockSeparator);
			for(String row : rowBlock) {
				String [] rowColumnVals = row.split(tokenSeparator);
			

					Object[] sensorData = new Object[sensorRec.getNumberProperties()];

					// verify that the number of columns for this entry is 1 greater than
					// the number of properties (the sensor id is also a column)
					if(rowColumnVals.length == sensorData.length) {
						int objectIndex = 0;
						for(int i = 0; i < rowColumnVals.length; i++){

							if(sensorRec.getModeledProperties().get(objectIndex).getPropertyType() == PropertyType.QUANTITY) {
								String nil = sensorRec.getModeledProperties().get(objectIndex).getNillValue();
								if(nil != null && rowColumnVals[i].trim().equals(nil.trim())){
									sensorData[objectIndex] = Double.NaN;
								}
								else{
									if(!rowColumnVals[i].trim().equals(""))
										sensorData[objectIndex] = Double.valueOf(rowColumnVals[i]);
								}
							}
							else
								sensorData[objectIndex] = rowColumnVals[i];
							objectIndex ++;
						}

					}
					sensorRec.addSensorDataRecord(sensorData, 0);
				
			}
		}
	}
	
	private void parseDynamicSensorDataValues(Element valuesElem, SweDataRecord sweRecord, String decimalSep,
											  String tokenSep, String blockSep, SweFileType parseFileType)
													  throws NumberFormatException{

	
		if(valuesElem != null){
			int timeColumn = sweRecord.getTimeColumn();
			int sensorStartColumn = sweRecord.getSectorStartColumn();
			String valueBlock = valuesElem.getValue();
			String[] rowBlock = valueBlock.split(blockSep);
			for(String row : rowBlock) {
				String [] rowColumnVals = row.split(tokenSep);
				if(timeColumn < rowColumnVals.length && 
						sensorStartColumn < rowColumnVals.length){
					String sensorId = rowColumnVals[sensorStartColumn];
					SensorModel sensorModel = sweRecord.getSensorIdtoSensorDataMap().get(sensorId);
					if(sensorModel != null) {
						SensorDataRecords sensorRec = sensorModel.getSensorDataRecord(); 
						if(sensorRec != null ){

							if(parseFileType == SweFileType.TIME_SERIES) {
								Object[] sensorData = new Object[sensorRec.getNumberProperties()];


								// verify that the number of columns for this entry is 1 greater than
								// the number of properties (the sensor id is also a column)
								if(rowColumnVals.length == sensorData.length + 1) {
									int objectIndex = 0;
									for(int i = 0; i < rowColumnVals.length; i++){
										if(i == sensorStartColumn)
											continue;
										if(sensorRec.getModeledProperties().get(objectIndex).getPropertyType() == PropertyType.QUANTITY) {
											String nil = sensorRec.getModeledProperties().get(objectIndex).getNillValue();
											if(nil != null && rowColumnVals[i].trim().equals(nil.trim())){
												sensorData[objectIndex] = Double.NaN;
											}
											else
												sensorData[objectIndex] = Double.valueOf(rowColumnVals[i]);
										}
										else
										sensorData[objectIndex] = rowColumnVals[i];
									objectIndex ++;
								}

							}
							sensorRec.addSensorDataRecord(sensorData, 0);
						}
						else {
								int objectIndex = 0;
								Integer countIndex = sensorRec.getPropertyIndex(PropertyType.COUNT);
								if(countIndex <= sensorStartColumn )
									countIndex++;
								if(countIndex != null){
									int numberRowRecords;
									int countVal = Integer.valueOf(rowColumnVals[countIndex]);
									int timeIndex = sensorRec.getPropertyIndex(PropertyType.TIME);
									boolean timeGlobalField = true; 
								
									numberRowRecords =  (sensorRec.getNumberProperties() - 2);

									// check to see if time index repeats
									if(timeIndex > countIndex ){								
										timeGlobalField = false;
									}
									// first compute the
									Object[] sensorData = null;
									Integer currentBinIndex = null;
									int dataRecordIndex = 0;
									
									for(int i = 0; i < rowColumnVals.length; i++){
										if(i == sensorStartColumn )
											continue;
								
										PropertyType currProperty = sensorRec.getModeledProperties().get(objectIndex).getPropertyType(); 
										if(currProperty == PropertyType.INDEX){
											if(sensorData != null && currentBinIndex != null) 
												sensorRec.addSensorDataRecord(sensorData, currentBinIndex);
											sensorData = new Object[numberRowRecords];
											currentBinIndex = Integer.valueOf(rowColumnVals[i]);
											dataRecordIndex = -1;
											if(timeGlobalField) {
												sensorData[0] = rowColumnVals[timeIndex];
												dataRecordIndex++;
											}
										}
										else {
											// check for errors in the data and break if the 
											// 
											if(sensorData != null && dataRecordIndex >= sensorData.length) {
												break;
											}
											if(currProperty == PropertyType.QUANTITY && sensorData != null) {
												String nil = sensorRec.getModeledProperties().get(objectIndex).getNillValue();

												if(nil != null && rowColumnVals[i].trim().equals(nil.trim())){
													sensorData[dataRecordIndex] = Double.NaN;
												}
												else
													sensorData[dataRecordIndex] = Double.valueOf(rowColumnVals[i]);
											}
											else if(sensorData != null)
												sensorData[dataRecordIndex] = rowColumnVals[i];
										}
										objectIndex ++;
										if(objectIndex >= sensorRec.getModeledProperties().size())
											objectIndex = countVal;
										dataRecordIndex++;
										
									}
									if(sensorData != null && currentBinIndex != null)
										sensorRec.addSensorDataRecord(sensorData, currentBinIndex);

								}
						}
					}
						}
				}

				
			}
			
		}
	}
	
	private void processNilValue(SensorProperty sensorData, Element parentElem){
		if(parentElem != null){
			Element nilElem = parentElem.getChild(nilValuesTag, swe2Ns);
			if(nilElem != null){
				for(Element nil : nilElem.getChildren()) {
					sensorData.setNillValue(nil.getChildText(nilValueTag, swe2Ns));
				}
			}
		}


	}

	private List<SensorProperty> parseQuantityField(Element quantityElem){
		List<SensorProperty> propertyList = new ArrayList<SensorProperty>();
		List<QualityModel> qualityMod = null;
		if(quantityElem != null && quantityElem.getName().equals(quantityTag)){

				SensorProperty sensorData = new SensorProperty();

				String sensorTypeDef = quantityElem.getAttributeValue(attDefinitionTag);
				qualityMod = parseQualityRecord(quantityElem);
				
				Element uom = quantityElem.getChild(uomTag, swe2Ns);
				if(uom != null) {
					String uomStr = uom.getAttributeValue(codeTag);
					sensorData.setSensorUnitOfMeasure(uomStr);
				}
				processNilValue(sensorData, quantityElem);
				sensorData.setSensorType(sensorTypeDef);
				sensorData.setPropertyType(PropertyType.QUANTITY);
				propertyList.add(sensorData);

				// create a new record for each quality
				if(qualityMod != null){
					for(QualityModel quality : qualityMod){
						SensorProperty sensorDataForQual = sensorData.getCopy();
						sensorDataForQual.setQualityMeasure(quality);
						sensorDataForQual.setPropertyType(PropertyType.QUALITY);
						propertyList.add(sensorDataForQual);
					}
				}
			
			
		}
		return propertyList;
	}
	
	
	private List<SensorProperty> processDataArrayRecord(Element dataArray){
		List<SensorProperty> propertyList = new ArrayList<SensorProperty>();
		if(dataArray != null){
			
			for(Element childField : dataArray.getChildren()){
				if(childField.getName().equals(elementCountTag) && childField.getValue().trim().equals("")){
					
					SensorProperty sensorData = new SensorProperty();
					// TODO save off the allowed interval
					sensorData.setPropertyType(PropertyType.COUNT);
					propertyList.add(sensorData);

				}
				else if(childField.getName().equals(elementTypeTag)){
					Element dataRec = childField.getChild(dataRecordTag, swe2Ns);
					propertyList.addAll(getColumnsFromDataRecordElem(dataRec));
											
				}
		
			}
			
			
		}
		return propertyList;
	}
	
	private List<SensorProperty> determineDataColumnFields(Element sensorField){
		List<QualityModel> qualityMod = null;
		List<SensorProperty> sensorProperty = new ArrayList<SensorProperty>();
		// parse the sensor field and initialize the sensorData object
		for(Element childField : sensorField.getChildren()){

			if(childField != null){

				if(childField.getName().equals(dataArrayTag)){
					 sensorProperty.addAll(processDataArrayRecord(childField));
				}
				else { 
				
					sensorProperty.addAll(parseQuantityField(childField));
			
					if(childField.getName().equals(countTag)){
						SensorProperty sensorData = new SensorProperty();
						// TODO save off the allowed interval
						sensorData.setPropertyType(PropertyType.INDEX);
						sensorProperty.add(sensorData);

					}
				

				}
				
			
				
			}
		}
		
	
		return sensorProperty;
	}
	
	private List<SensorProperty> getColumnsFromDataRecordElem(Element dataRecord){
		List<SensorProperty> sensorProperty = new ArrayList<SensorProperty>();
		for(Element sensorField : dataRecord.getChildren(fieldTag, swe2Ns)){
//			if(sensorField.getChild(elementCountTag, swe2Ns) != null){
//				System.out.println("found COUNT Tag");
//				SensorProperty sensorData = new SensorProperty();
//				sensorData.setPropertyType(PropertyType.COUNT);
//				sensorProperty.add(sensorData);
//			}
//			else
				sensorProperty.addAll(determineDataColumnFields(sensorField));
			

		}
//		System.out.println("PROP - "+ sensorProperty.size() );
//		for(SensorProperty prop : sensorProperty)
//			System.out.println(prop.getPropertyType().toString());
		return sensorProperty;
	}
	
	private void processDynamicSensorData(Element dataChoiceElem, SweDataRecord sweRecord){
	
		for(Element sensorItem : dataChoiceElem.getChildren(itemTag, swe2Ns)){
			String sensorId = sensorItem.getAttributeValue(attNameTag, swe2Ns);
	
			List<SensorProperty> sensorProperty = new ArrayList<SensorProperty>();
			for(Element dataRecordElem : sensorItem.getChildren()){
				sensorProperty.addAll(getColumnsFromDataRecordElem(dataRecordElem));
			}
			SensorDataRecords sensorDataRecord = new SensorDataRecords(sensorId, sensorProperty);
			SensorModel sensModel = sweRecord.getSensorIdtoSensorDataMap().get(sensorId);
			if(sensModel !=  null)
				sensModel.setSensorDataRecord(sensorDataRecord);

		}
	}
	
	private List<QualityModel> parseQualityRecord(Element parentElem){
		List<QualityModel> qualityMod =  null;
		if(parentElem != null){
			for(Element qualityElem :  parentElem.getChildren(qualityTag, swe2Ns)){
				if(qualityMod == null)
					qualityMod = new ArrayList<QualityModel>();
				for(Element catElem : qualityElem.getChildren()){
					//if(categoryDefinition.equals(catElem.getAttributeValue(attDefinitionTag)) ||
						//	parentElem.getName().equals(countTag)){
						QualityModel model = new QualityModel();

						Element childElem = catElem.getChild(labelTag, swe2Ns);
						if(childElem != null) {
							model.setQualityLabel(childElem.getValue());
						}
						childElem = catElem.getChild(codeSpaceTag, swe2Ns);
						if(childElem !=  null){
							model.setReferenceLink(childElem.getAttributeValue(hrefTag, xlinkNs));
						}
						childElem = catElem.getChild(constraintTag, swe2Ns);
						if(childElem != null){
							Element allowedTokens = childElem.getChild(allowedTokenTag, swe2Ns);
							if(allowedTokens != null){
								model.setAllowedTokensId(allowedTokens.getAttributeValue(idTag));
								for(Element tokElem : allowedTokens.getChildren()){
									model.addAllowedToken(tokElem.getValue());
								}

							}
						}
						qualityMod.add(model);
						
					//}
				}

			}

		}
		return qualityMod;
		
	}
	
	private int parseElementCount(Element elementCount){
		SweDataRecord tempRecord = new SweDataRecord();
		parseElementCount(elementCount, tempRecord);
		return (tempRecord.getNumberDataRows());
	}
	
	private List<QualityModel> parseElementCount(Element elementCount,  SweDataRecord sweRecord){
		Integer dataRows = 0;
		List<QualityModel> qualityModel = null;
		if(elementCount != null) {
			Element count = elementCount.getChild(countTag, swe2Ns);
			if(count != null) {
				qualityModel = parseQualityRecord(count);
				String dataRowStr = count.getChildText(valueTag, swe2Ns);
				try{
					dataRows = Integer.valueOf(dataRowStr);
				}
				catch(NumberFormatException e){
					e.printStackTrace();
				}
			}
		}
		sweRecord.setNumberDataRows(dataRows);
		return qualityModel;
	}
	
}