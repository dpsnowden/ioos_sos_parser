package com.asascience.ioos.parser;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.jdom2.Document;
import org.jdom2.Element;
import org.jdom2.JDOMException;
import org.jdom2.input.SAXBuilder;
import org.joda.time.DateTime;


import com.asascience.ioos.exception.IoosSosParserException;
import com.asascience.ioos.model.AddressModel;
import com.asascience.ioos.model.BoundingBox;
import com.asascience.ioos.model.LatLonPoint;
import com.asascience.ioos.model.capabilities.DCP;
import com.asascience.ioos.model.capabilities.GetCapabilities;
import com.asascience.ioos.model.capabilities.Observation;
import com.asascience.ioos.model.capabilities.Operation;
import com.asascience.ioos.model.capabilities.ServiceIdentification;
import com.asascience.ioos.model.capabilities.ServiceProvider;


public class GetCapabilitiesParser extends BaseParser {
	private final String version1_0 = "1.0";
	private final String serviceIdTag = "ServiceIdentification";
	private final String titleTag = "Title";
	private final String abstractTag = "Abstract";
	private final String keywordsTag = "Keywords";
	private final String keywordTag = "Keyword";
	private final String serviceTypeTag = "ServiceType";
	private final String serviceTypeVersionTag = "ServiceTypeVersion";
	private final String feeTag = "Fees";
	private final String accessConstraintsTag = "AccessConstraints";
	private final String serviceProviderTag = "ServiceProvider";
	private final String providerNameTag = "ProviderName";
	private final String providerSiteTag = "ProviderSite";
	private final String serviceContactTag = "ServiceContact";
	private final String individualNameTag = "IndividualTName";
	private final String voiceTag = "Voice";
	private final String operationsMetadataTag = "OperationsMetadata";
	private final String operationTag = "Operation";
	private final String dcpTag = "DCP";
	private final String getTag = "Get";
	private final String postTag ="Post";
	private final String paramaterTag = "Parameter";
	private final String allowedValuesTag = "AllowedValues";
	private final String extendedCapaTag = "ExtendedCapabilities";
	private final String contentsTag = "Contents";
	private final String observationOfferingListTag = "ObservationOfferingList";
	private final String observationOfferingTag = "ObservationOffering";
	private final String procedureTag = "procedure";
	private final String observedPropertyTag = "observedProperty";
	private final String featureOfInterestTag = "featureOfInterest";
	private final String responseFormatTag = "responseFormat";
	private final String resultModelTag = "resultModel";
	private final String responseModeTag = "responseMode";
	private final String timeTag = "timeTag";
	
	public GetCapabilitiesParser(){
		
	}
	

	private ServiceProvider parseServiceProvider(Element serviceProvider){
		ServiceProvider provider = new ServiceProvider();

		if(serviceProvider != null){
			for(Element providerElem : serviceProvider.getChildren()){
				switch(providerElem.getName()){

				case providerNameTag:
					provider.setContactName(providerElem.getText());
					break;
				case providerSiteTag:
					provider.setProviderUrl(providerElem.getAttributeValue(xlinkAttributeHrefTag, xlinkNs));
				case serviceContactTag:
					for(Element contactElem : providerElem.getChildren()){
						if(contactElem.getName().equals(individualNameTag)){
							provider.setContactName(contactElem.getText());
						}
						else if(contactElem.getName().equals(owsContactInfoTag)){
							parseContactInfo(contactElem, provider);
						
							
						}
					}
				}
			}
		}
		return provider;
	}
	
	
	
	
	
	private ServiceIdentification parseServiceIdentification(Element serviceElem){
		ServiceIdentification serviceId = new ServiceIdentification();
		if(serviceElem != null){
			for(Element serviceElemChild : serviceElem.getChildren()){
				switch (serviceElemChild.getName()){
				case titleTag:
					serviceId.setTitle(serviceElemChild.getText());
					break;
				case abstractTag:
					serviceId.setAbstractOfService(serviceElemChild.getText());
					break;
				case keywordsTag:
					for(Element keyElem : serviceElemChild.getChildren(keywordTag, owsNs)){
						if(keyElem.getText() != null)
							serviceId.getKeywordList().add(keyElem.getText());
					}
					break;
				case serviceTypeTag:
					serviceId.setServiceType(serviceElemChild.getText());
					break;
				
				case serviceTypeVersionTag:
					serviceId.setServiceTypeVersion(serviceElemChild.getText());
					break;
				case feeTag:
					serviceId.setFeesForService(serviceElemChild.getText());
					break;
				case accessConstraintsTag:
					serviceId.setAccessConstraints(serviceElemChild.getText());
					break;
				}
				
				
			}
			
		}
		return serviceId;
	}
	
	
	private void parseParameterElement(Map<String, List<String>> parameterMap, Element paramElem){
		if(paramElem != null){
			Element allowedVals = paramElem.getChild(allowedValuesTag, owsNs);
			if(allowedVals != null){
				String paramName = paramElem.getAttributeValue(nameTag);
				List<String> allowedValList = new ArrayList<String>();
				for(Element valElem : allowedVals.getChildren()){
					allowedValList.add(valElem.getText());
				}
				parameterMap.put(paramName, allowedValList);

			}
		}
	}
	
	
	private void parseOperationsMetadata(Element operationElement, 	GetCapabilities getCap){
		List<Operation> operationList = new ArrayList<Operation>();
		if(operationElement != null){
			for(Element operationElem : operationElement.getChildren()){
				Operation operation = new Operation();

				if(operationElem.getNamespace().equals(owsNs)){
					if(operationElem.getName().equals(operationTag)){
						operation.setOperationName(operationElem.getAttributeValue(nameTag));

						for(Element childElem : operationElem.getChildren()){
							if(childElem.getName().equals(dcpTag)){
								for(Element dcpElem : childElem.getChildren()){
									DCP dcp = new DCP();
									dcp.setDcpType(dcpElem.getName());
									for(Element dataElem : dcpElem.getChildren()){
										if(dataElem.getNamespace().equals(owsNs)){
											if(dataElem.getName().equals(getTag)){
												dcp.setGetLink(dataElem.getAttributeValue(xlinkAttributeHrefTag, xlinkNs));
											}
											else if(dataElem.getName().equals(postTag)){
												dcp.setPostLink(dataElem.getAttributeValue(xlinkAttributeHrefTag, xlinkNs));

											}
										}
									}
									operation.getDcpList().add(dcp);
								}
							}
							else if(childElem.getName().equals(paramaterTag)){
								 parseParameterElement(operation.getParameterAllowedValues(), childElem);
				
							}			
						}
					}
					else if(operationElem.getName().equals(paramaterTag)){
						 parseParameterElement(getCap.getCapabilitiesParameters(), operationElem);
		
					}
					else if(operationElem.getName().equals(extendedCapaTag)){
						Element gmlMeta = operationElem.getChild(gmlMetaDataTag, gmlNs);
						if(gmlMeta != null){
								getCap.setVersion(gmlMeta.getChildText(gmlVersionTag, gmlNs));
								getCap.setRefTitle(gmlMeta.getAttributeValue(titleTag, xlinkNs));
								getCap.setRefLink(gmlMeta.getAttributeValue( 
										xlinkAttributeHrefTag, xlinkNs));
							
						}
					}
					
				}
				operationList.add(operation);

			}

		}
		getCap.setOperationsList(operationList);
	}
	
	private List<Observation> parseOfferingList(Element parseObservationList){
		List<Observation> obsList = new ArrayList<Observation>();
		if(parseObservationList != null){
			for(Element obs : parseObservationList.getChildren(observationOfferingTag, sosNs)){
				Observation obsModel = parseObservation(obs);
				if(obsModel != null)
					obsList.add(obsModel);
			}
			
		}
		return obsList;
	}
	
	private Observation parseObservation(Element observation){
		Observation obs = null;
		if(observation != null){
			obs = new Observation();
			obs.setOfferingID(observation.getAttributeValue(gmlIdTag, gmlNs));
			obs.setDescription(observation.getChildText(gmlDescriptionTag, gmlNs));
			obs.setObservationName(observation.getChildText(nameTag, gmlNs));
			obs.setObservationSrsName(observation.getChildText(gmlSrsNameTag, gmlNs));
			obs.setResponseFormat(observation.getChildText(responseFormatTag, sosNs));
			obs.setResultModel(observation.getChildText(resultModelTag, sosNs));
			obs.setResponseMode(observation.getChildText(responseModeTag, sosNs));
			
			Element childElem = observation.getChild(procedureTag, sosNs);
			if(childElem != null)
				obs.setProcedureLink(childElem.getAttributeValue(xlinkAttributeHrefTag, xlinkNs));
			
			childElem = observation.getChild(featureOfInterestTag, sosNs);
			if(childElem != null)
				obs.setFeatureOfInterestLink(childElem.getAttributeValue(xlinkAttributeHrefTag, xlinkNs));
			
			for(Element obsProp : observation.getChildren(observedPropertyTag, sosNs)){
				obs.getObservedPropertiesRefList().add(
						obsProp.getAttributeValue(xlinkAttributeHrefTag, xlinkNs));
			}
		
			obs.setBoundingBox(parseBoundedBy(observation.getChild(gmlBoundedByTag, gmlNs)));

			childElem = observation.getChild(timeTag, sosNs);
			if(childElem != null){
				// Get the start and end time periods
				Element timePeriod = childElem.getChild(gmlTimePeriod, gmlNs);
				if(timePeriod != null){
					DateTime dateTime;
					Element beginPosE = timePeriod.getChild(gmlBeginPosition, gmlNs);
					if(beginPosE != null){
						String indTime = beginPosE.getAttributeValue(indeterminatePosTag);
						if(indTime != null && indTime.equals(timeNowTag)){
							dateTime = DateTime.now();
						}
						else{
							String beginPos = timePeriod.getChildText(gmlBeginPosition, gmlNs);
							dateTime = DateTime.parse(beginPos);
						}
						obs.setStartSamplingTime(dateTime);

					}
					
					Element endPosE = timePeriod.getChild(gmlEndPosition, gmlNs);
					if(endPosE != null){
						String indTime = beginPosE.getAttributeValue(indeterminatePosTag);
						if(indTime != null && indTime.equals(timeNowTag)){
							dateTime = DateTime.now();
						}
						else{
							String beginPos = timePeriod.getChildText(gmlEndPosition, gmlNs);
							dateTime = DateTime.parse(beginPos);
						}
						obs.setEndSamplingTime(dateTime);

					}
				}

			}
		}
		return obs;
	}

	public boolean isGetCapabilitiesVersion1(String fileName){
		boolean isGetCapVers1 = false;
		try {
			Document xmlDoc;
			File xmlFile = new File(fileName);
			xmlDoc = new SAXBuilder().build(xmlFile);
			Element root = xmlDoc.getRootElement();

			initNamespaces(root);

			GetCapabilities getCap = new GetCapabilities();
			parseOperationsMetadata(root.getChild(operationsMetadataTag, owsNs), getCap);
			if(getCap != null && version1_0.equals(getCap.getVersion()))
				isGetCapVers1 = true;
		} catch (JDOMException | IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return isGetCapVers1;
		
	}
	
	
	public GetCapabilities parseGetCapabilities(URL xmlUrl) throws IoosSosParserException{
		URLConnection connect;
		GetCapabilities getCap = null;

		try {
			connect = xmlUrl.openConnection();

			InputStream isReader = connect.getInputStream();
			if(isReader != null){
				getCap = new GetCapabilities();
				Document xmlDoc = new SAXBuilder().build(isReader);
				parseGetCapabilities(xmlDoc, getCap);

			}

		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (JDOMException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return getCap;
	}

	private void parseGetCapabilities(Document xmlDoc, GetCapabilities getCap) throws IoosSosParserException{
		Element root = xmlDoc.getRootElement();
		initNamespaces(root);
		getCap.setServiceId( parseServiceIdentification(root.getChild(serviceIdTag, owsNs)));
		getCap.setServiceProvider(parseServiceProvider(root.getChild(serviceProviderTag, owsNs)));
		parseOperationsMetadata(root.getChild(operationsMetadataTag, owsNs), getCap);
		
		Element content = root.getChild(contentsTag, sosNs);
		if(content != null)
			getCap.setObservationList(parseOfferingList(
					content.getChild(this.observationOfferingListTag, sosNs)));
		
	}
	
	public GetCapabilities parseGetCapabilities(String xmlFileName) 
			throws JDOMException, IOException, IoosSosParserException{
		File xmlFile = new File(xmlFileName);
		GetCapabilities getCap = null;
		if(xmlFile.exists()){

			getCap = new GetCapabilities();
			Document xmlDoc = new SAXBuilder().build(xmlFile);
			parseGetCapabilities(xmlDoc, getCap);
		}
		return getCap;
	}
}
