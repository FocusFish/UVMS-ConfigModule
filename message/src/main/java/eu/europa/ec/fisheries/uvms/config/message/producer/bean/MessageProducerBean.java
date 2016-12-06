/*
﻿Developed with the contribution of the European Commission - Directorate General for Maritime Affairs and Fisheries
© European Union, 2015-2016.

This file is part of the Integrated Fisheries Data Management (IFDM) Suite. The IFDM Suite is free software: you can
redistribute it and/or modify it under the terms of the GNU General Public License as published by the
Free Software Foundation, either version 3 of the License, or any later version. The IFDM Suite is distributed in
the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License for more details. You should have received a
copy of the GNU General Public License along with the IFDM Suite. If not, see <http://www.gnu.org/licenses/>.
 */
package eu.europa.ec.fisheries.uvms.config.message.producer.bean;

import javax.annotation.PostConstruct;
import javax.annotation.Resource;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import javax.enterprise.event.Observes;
import javax.jms.*;

import eu.europa.ec.fisheries.uvms.message.JMSUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import eu.europa.ec.fisheries.uvms.config.message.constants.DataSourceQueue;
import eu.europa.ec.fisheries.uvms.config.message.constants.MessageConstants;
import eu.europa.ec.fisheries.uvms.config.message.event.ErrorEvent;
import eu.europa.ec.fisheries.uvms.config.message.event.EventMessage;
import eu.europa.ec.fisheries.uvms.config.message.exception.MessageException;
import eu.europa.ec.fisheries.uvms.config.message.producer.MessageProducer;
import eu.europa.ec.fisheries.uvms.config.model.exception.ModelMarshallException;
import eu.europa.ec.fisheries.uvms.config.model.mapper.JAXBMarshaller;
import javax.ejb.Singleton;
import javax.naming.InitialContext;
import javax.naming.NamingException;

@Singleton
public class MessageProducerBean implements MessageProducer {

    final static Logger LOG = LoggerFactory.getLogger(MessageProducerBean.class);

    private Queue auditQueue;
    private Queue responseQueue;

    private Topic configTopic;

    private ConnectionFactory connectionFactory;

    private Connection connection = null;
    private Session session = null;

    @PostConstruct
    public void init() {
        InitialContext ctx;
        try {
            ctx = new InitialContext();
        } catch (Exception e) {
            LOG.error("Failed to get InitialContext",e);
            throw new RuntimeException(e);
        }
        lookupConnectionFactory(ctx);
        responseQueue = JMSUtils.lookupQueue(ctx, MessageConstants.COMPONENT_RESPONSE_QUEUE);
        auditQueue = JMSUtils.lookupQueue(ctx, MessageConstants.AUDIT_MODULE_QUEUE);
        configTopic = JMSUtils.lookupTopic(ctx, MessageConstants.CONFIG_STATUS_TOPIC);
    }

    private void lookupConnectionFactory(InitialContext ctx) {
        LOG.debug("Open connection to JMS broker");
        try {
            connectionFactory = (QueueConnectionFactory) ctx.lookup(MessageConstants.CONNECTION_FACTORY);
        } catch (NamingException ne) {
            //if we did not find the connection factory we might need to add java:/ at the start
            LOG.debug("Connection Factory lookup failed for " + MessageConstants.CONNECTION_FACTORY);
            String wfName = "java:/" + MessageConstants.CONNECTION_FACTORY;
            try {
                LOG.debug("trying " + wfName);
                connectionFactory = (QueueConnectionFactory) ctx.lookup(wfName);
            } catch (Exception e) {
                LOG.error("Connection Factory lookup failed for both " + MessageConstants.CONNECTION_FACTORY  + " and " + wfName);
                throw new RuntimeException(e);
            }
        }
        try {
            connection = connectionFactory.createConnection();
            connection.start();
        } catch (JMSException ex) {
            LOG.error("Error when open connection to JMS broker");
        }
    }
    
    @Override
    @TransactionAttribute(TransactionAttributeType.REQUIRES_NEW)
    public String sendDataSourceMessage(String text, DataSourceQueue queue) throws MessageException {
        try {
            connectToJMS();
            TextMessage message = session.createTextMessage();
            message.setJMSReplyTo(responseQueue);
            message.setText(text);

            switch (queue) {
                case AUDIT:
                    getProducer(session, auditQueue).send(message);
                    break;
                default:
                	break;
            }

            return message.getJMSMessageID();
        } catch (Exception e) {
            LOG.error("[ Error when sending message. ] {0}", e.getMessage());
            throw new MessageException("[ Error when sending message. ]", e);
        } finally {
            disconnectFromJMS();
        }
    }

    public void sendConfigDeployedMessage(String text) throws MessageException {
        try {
            connectToJMS();
            TextMessage message = session.createTextMessage(text);
            getProducer(session, configTopic).send(message);
        }
        catch (Exception e) {
            LOG.error("[ Error when sending config deployed message. ] {}", e.getMessage());
            throw new MessageException("Error when sending config deployed message.", e);
        }
        finally {
            disconnectFromJMS();
        }
    }

    @Override
    @TransactionAttribute(TransactionAttributeType.REQUIRES_NEW)
    public void sendModuleResponseMessage(TextMessage requestMessage, String response) throws MessageException {
        sendReply(requestMessage, response);
    }

    @Override
    @TransactionAttribute(TransactionAttributeType.REQUIRES_NEW)
    public void sendModuleErrorMessage(@Observes @ErrorEvent EventMessage eventMessage) throws MessageException {
        try {
            String faultString = JAXBMarshaller.marshallJaxBObjectToString(eventMessage.getFault());
            sendReply(eventMessage.getJmsMessage(), faultString);
        } catch (ModelMarshallException e) {
            LOG.error("[ Error when sending module error message. ] {}", e.getMessage());
            throw new MessageException("[ Error when sending module error message. ]", e);
        }
    }

    private void sendReply(TextMessage requestMessage, String response) throws MessageException {
        try {
            connectToJMS();
            TextMessage responseMessage = session.createTextMessage(response);
            responseMessage.setJMSCorrelationID(requestMessage.getJMSMessageID());
            getProducer(session, requestMessage.getJMSReplyTo()).send(responseMessage);
        }
        catch (JMSException e) {
            LOG.error("[ Error when replying to module message. ] {}", e.getMessage());
            throw new MessageException("Error when replying to module message.", e);
        }
        finally {
            disconnectFromJMS();
        }        
    }

    private void connectToJMS() throws JMSException {
        connection = connectionFactory.createConnection();
        session = connection.createSession(false, Session.AUTO_ACKNOWLEDGE);
        connection.start();
    }

    private void disconnectFromJMS() {
        try {
            if (connection != null) {
                connection.stop();
                connection.close();
            }
        }
        catch (JMSException e) {
            LOG.error("[ Error when closing JMS connection ] {}", e.getMessage());
        }
    }

    private javax.jms.MessageProducer getProducer(Session session, Destination destination) throws JMSException {
        javax.jms.MessageProducer producer = session.createProducer(destination);
        producer.setDeliveryMode(DeliveryMode.NON_PERSISTENT);
        producer.setTimeToLive(60000L);
        return producer;
    }
}