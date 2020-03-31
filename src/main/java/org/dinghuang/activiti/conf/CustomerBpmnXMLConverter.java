package org.dinghuang.activiti.conf;

import org.activiti.bpmn.converter.BaseBpmnXMLConverter;
import org.activiti.bpmn.model.*;
import org.activiti.bpmn.model.Process;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Configuration;

import javax.xml.stream.XMLStreamReader;
import java.util.List;

/**
 * @author dinghuang123@gmail.com
 * @since 2020/3/31
 */
@Configuration
public abstract class CustomerBpmnXMLConverter extends BaseBpmnXMLConverter {

    protected static final Logger LOGGER = LoggerFactory.getLogger(BaseBpmnXMLConverter.class);

    @Override
    public void convertToBpmnModel(XMLStreamReader xtr, BpmnModel model, Process activeProcess, List<SubProcess> activeSubProcessList) throws Exception {

        String elementId = xtr.getAttributeValue(null, ATTRIBUTE_ID);
        String elementName = xtr.getAttributeValue(null, ATTRIBUTE_NAME);
        boolean async = parseAsync(xtr);
        boolean notExclusive = parseNotExclusive(xtr);
        String defaultFlow = xtr.getAttributeValue(null, ATTRIBUTE_DEFAULT);
        boolean isForCompensation = parseForCompensation(xtr);

        BaseElement parsedElement = convertXMLToElement(xtr, model);

        if (parsedElement instanceof Artifact) {
            Artifact currentArtifact = (Artifact) parsedElement;
            currentArtifact.setId(elementId);

            if (!activeSubProcessList.isEmpty()) {
                activeSubProcessList.get(activeSubProcessList.size() - 1).addArtifact(currentArtifact);

            } else {
                activeProcess.addArtifact(currentArtifact);
            }
        }

        if (parsedElement instanceof FlowElement) {

            FlowElement currentFlowElement = (FlowElement) parsedElement;
            currentFlowElement.setId(elementId);
            currentFlowElement.setName(elementName);

            if (currentFlowElement instanceof FlowNode) {
                FlowNode flowNode = (FlowNode) currentFlowElement;
                flowNode.setAsynchronous(async);
                flowNode.setNotExclusive(notExclusive);

                if (currentFlowElement instanceof Activity) {

                    Activity activity = (Activity) currentFlowElement;
                    activity.setForCompensation(isForCompensation);
                    if (StringUtils.isNotEmpty(defaultFlow)) {
                        activity.setDefaultFlow(defaultFlow);
                    }
                }

                if (currentFlowElement instanceof Gateway) {
                    Gateway gateway = (Gateway) currentFlowElement;
                    if (StringUtils.isNotEmpty(defaultFlow)) {
                        gateway.setDefaultFlow(defaultFlow);
                    }
                }
            }

            if (currentFlowElement instanceof DataObject) {
                if (!activeSubProcessList.isEmpty()) {
                    SubProcess subProcess = activeSubProcessList.get(activeSubProcessList.size() - 1);
                    subProcess.getDataObjects().add((ValuedDataObject) parsedElement);
                } else {
                    activeProcess.getDataObjects().add((ValuedDataObject) parsedElement);
                }
            }

            if (!activeSubProcessList.isEmpty()) {

                SubProcess subProcess = activeSubProcessList.get(activeSubProcessList.size() - 1);
                subProcess.addFlowElement(currentFlowElement);

            } else {
                activeProcess.addFlowElement(currentFlowElement);
            }
        }
    }

}
