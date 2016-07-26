package io.bigdime.handler.hive;

import java.io.IOException;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import com.google.common.base.Preconditions;

import io.bigdime.alert.LoggerFactory;
import io.bigdime.core.ActionEvent;
import io.bigdime.core.ActionEvent.Status;
import io.bigdime.core.AdaptorConfigurationException;
import io.bigdime.core.HandlerException;
import io.bigdime.core.InvalidValueConfigurationException;
import io.bigdime.core.commons.AdaptorLogger;
import io.bigdime.core.commons.ProcessHelper;
import io.bigdime.core.commons.PropertyHelper;
import io.bigdime.core.config.AdaptorConfigConstants;
import io.bigdime.core.constants.ActionEventHeaderConstants;
import io.bigdime.core.handler.AbstractHandler;
import io.bigdime.core.runtimeinfo.RuntimeInfo;
import io.bigdime.core.runtimeinfo.RuntimeInfoStore;
import io.bigdime.core.runtimeinfo.RuntimeInfoStoreException;
import io.bigdime.libs.hdfs.HDFS_AUTH_OPTION;
import io.bigdime.libs.hdfs.jdbc.HiveJdbcConnectionFactory;

/**
 * 
 * HiveReaderHandler reads data from a hive table/query and outputs each row as
 * one event.
 * 
 * HiveReaderHandler: Configure one source for each recurring file. Store data
 * in file. Send the output filename in the header. File reader handler to get
 * the filename from the header. SwiftWriter
 * 
 * src-desc : { entity-name query
 * 
 * "input1": { "entity-name": "tracking_events", "hive-query": "query"
 * "hive-conf": { "mapred.job.queue.name" : "queuename",
 * "mapred.output.compress" : "true", "hive.exec.compress.output" : "true",
 * "mapred.output.compression.codec" :
 * "org.apache.hadoop.io.compress.GzipCodec", "io.compression.codecs" :
 * "org.apache.hadoop.io.compress.GzipCodec", "mapred.reduce.tasks" : "500"
 * 
 * "schemaFileName" :"${hive_schema_file_name}" },
 * 
 * "input1" : "table1: table1 ", "input2" : "table2: table2"
 * 
 * 
 * }
 * 
 * 
 * @author Neeraj Jain
 *
 */
@Component
@Scope("prototype")
public class HiveJdbcReaderHandler extends AbstractHandler {
	private static final AdaptorLogger logger = new AdaptorLogger(LoggerFactory.getLogger(HiveJdbcReaderHandler.class));
	// private String handlerPhase = "building HiveReaderHandler";

	private String outputDirectory = "";

	private String jdbcUrl = null;// e.g.
									// jdbc:hive2://host:10000/default;principal=hadoop/host@DOMAIN.COM
	private String driverClassName = null;
	private HDFS_AUTH_OPTION authOption;

	private String userName = null;// e.g. username
	private String password = null; // e.g. password

	private String baseOutputDirectory = null;

	private String entityName = null;
	private String hiveQuery = null;
	private final Map<String, String> hiveConfigurations = new HashMap<>();

	final DateTimeFormatter jobDtf = DateTimeFormat.forPattern("yyyyMMddHHmmssSSS");

	final DateTimeFormatter hiveQueryDtf = DateTimeFormat.forPattern("yyyy-MM-dd");
	final DateTimeFormatter hdfsOutputPathDtf = DateTimeFormat.forPattern("yyyyMMdd");

	final private int DEFAULT_GO_BACK_DAYS = 1;

	/**
	 * How many days should we go back to process the records. 0 means process
	 * todays records, 1 means process yesterdays records
	 */
	private int goBackDays = DEFAULT_GO_BACK_DAYS;
	@Autowired
	HiveJdbcConnectionFactory hiveJdbcConnectionFactory;

	private Connection connection = null;

	// private long processEntryTime = 0;

	// private long goBackDaysSameTime = 0;
	@Autowired
	private RuntimeInfoStore<RuntimeInfo> runtimeInfoStore;

	long dirtyRecordCount = 0;
	List<RuntimeInfo> dirtyRecords;

	private boolean processingDirty = false;

	private HiveReaderDescriptor inputDescriptor;
	private long hiveConfDateTime;
	private long intervalInMins = 24 * 60;
	private long intervalInMillis = intervalInMins * 60 * 1000;

	private final String INPUT_DESCRIPTOR_PREFIX = "hiveConfDate:";

	@Override
	public void build() throws AdaptorConfigurationException {
		super.build();
		setHandlerPhase("building HiveJdbcReaderHandler");
		// handlerPhase = "building HiveJdbcReaderHandler";
		logger.info(getHandlerPhase(), "properties={}", getPropertyMap());

		Map<String, Object> properties = getPropertyMap();
		for (String key : properties.keySet()) {
			logger.debug(getHandlerPhase(), "key=\"{}\" value=\"{}\"", key, getPropertyMap().get(key));
		}

		// sanity check for src-desc
		@SuppressWarnings("unchecked")
		Entry<Object, String> srcDescEntry = (Entry<Object, String>) getPropertyMap()
				.get(AdaptorConfigConstants.SourceConfigConstants.SRC_DESC);
		if (srcDescEntry == null) {
			throw new InvalidValueConfigurationException("src-desc can't be null");
		}

		logger.debug(getHandlerPhase(), "src-desc-node-key=\"{}\" src-desc-node-value=\"{}\"", srcDescEntry.getKey(),
				srcDescEntry.getValue());

		@SuppressWarnings("unchecked")
		Map<String, Object> srcDescValueMap = (Map<String, Object>) srcDescEntry.getKey();

		entityName = PropertyHelper.getStringProperty(srcDescValueMap, HiveJdbcReaderHandlerConstants.ENTITY_NAME);
		hiveQuery = PropertyHelper.getStringProperty(srcDescValueMap, HiveJdbcReaderHandlerConstants.HIVE_QUERY);

		goBackDays = PropertyHelper.getIntProperty(getPropertyMap(), HiveJdbcReaderHandlerConstants.GO_BACK_DAYS,
				DEFAULT_GO_BACK_DAYS);

		logger.debug(getHandlerPhase(), "entityName=\"{}\" hiveQuery=\"{}\" goBackDays={}", entityName, hiveQuery,
				goBackDays);

		Preconditions.checkArgument(goBackDays >= 0,
				HiveJdbcReaderHandlerConstants.GO_BACK_DAYS + " has to be a non-negative value.");

		for (String key : srcDescValueMap.keySet()) {
			logger.debug(getHandlerPhase(), "srcDesc-key=\"{}\" srcDesc-value=\"{}\"", key, srcDescValueMap.get(key));
		}

		setHiveConfigurations(srcDescValueMap);

		// Set JDBC params
		jdbcUrl = PropertyHelper.getStringProperty(getPropertyMap(), HiveJdbcReaderHandlerConstants.JDBC_URL);
		driverClassName = PropertyHelper.getStringProperty(getPropertyMap(),
				HiveJdbcReaderHandlerConstants.DRIVER_CLASS_NAME);
		final String authChoice = PropertyHelper.getStringProperty(getPropertyMap(),
				HiveJdbcReaderHandlerConstants.AUTH_CHOICE, HDFS_AUTH_OPTION.KERBEROS.toString());

		authOption = HDFS_AUTH_OPTION.getByName(authChoice);

		userName = PropertyHelper.getStringProperty(getPropertyMap(),
				HiveJdbcReaderHandlerConstants.HIVE_JDBC_USER_NAME);
		password = PropertyHelper.getStringProperty(getPropertyMap(), HiveJdbcReaderHandlerConstants.HIVE_JDBC_SECRET);

		logger.debug(getHandlerPhase(),
				"jdbcUrl=\"{}\" driverClassName=\"{}\" authChoice={} authOption={} userName=\"{}\" password=\"****\"",
				jdbcUrl, driverClassName, authChoice, authOption, userName);

		baseOutputDirectory = PropertyHelper.getStringProperty(getPropertyMap(),
				HiveJdbcReaderHandlerConstants.BASE_OUTPUT_DIRECTORY, "/");

	}

	@SuppressWarnings("unchecked")
	private void setHiveConfigurations(Map<String, Object> srcDescValueMap) {
		if (getPropertyMap().get(HiveJdbcReaderHandlerConstants.HIVE_CONF) != null) {
			logger.debug(getHandlerPhase(), "found hive-conf in handler properties");
			hiveConfigurations
					.putAll(PropertyHelper.getMapProperty(getPropertyMap(), HiveJdbcReaderHandlerConstants.HIVE_CONF));
			logger.debug(getHandlerPhase(), "hiveConfs from handler properties=\"{}\"", hiveConfigurations);
		}

		if (PropertyHelper.getMapProperty(srcDescValueMap, HiveJdbcReaderHandlerConstants.HIVE_CONF) != null) {
			logger.debug(getHandlerPhase(), "found hive-conf in src-desc properties");
			hiveConfigurations
					.putAll(PropertyHelper.getMapProperty(srcDescValueMap, HiveJdbcReaderHandlerConstants.HIVE_CONF));
		}
		logger.debug(getHandlerPhase(), "hiveConfs=\"{}\"", hiveConfigurations);

	}

	/**
	 * If it's the first run, find the dirty records and add to the list.
	 * 
	 * After that, just set the next descriptor to process.
	 * 
	 * @return
	 * @throws IOException
	 * @throws RuntimeInfoStoreException
	 * @throws HandlerException
	 */
	@Override
	protected Status preProcess() throws IOException, RuntimeInfoStoreException, HandlerException {
		// if (isFirstRun()) {
		// dirtyRecords = getAllStartedRuntimeInfos(runtimeInfoStore,
		// entityName);
		//
		// final Iterator<RuntimeInfo> dirtyRecordIter =
		// dirtyRecords.iterator();
		// while (dirtyRecordIter.hasNext()) {
		// final RuntimeInfo dirtyRecord = dirtyRecordIter.next();
		//
		// if
		// (!dirtyRecord.getInputDescriptor().startsWith(INPUT_DESCRIPTOR_PREFIX))
		// {
		// logger.info(getHandlerPhase(),
		// "_message=\"removing from dirty record list\" handler_id={}
		// input_descriptor={} startWith={}",
		// getId(), dirtyRecord.getInputDescriptor(),
		// dirtyRecord.getInputDescriptor().startsWith(INPUT_DESCRIPTOR_PREFIX));
		// dirtyRecordIter.remove();
		// }
		// }
		//
		// if (dirtyRecords != null && !dirtyRecords.isEmpty()) {
		// dirtyRecordCount = dirtyRecords.size();
		// logger.warn(getHandlerPhase(),
		// "_message=\"dirty records found\" handler_id={}
		// dirty_record_count=\"{}\" entityName={}",
		// getId(), dirtyRecordCount, entityName);
		// } else {
		// logger.info(getHandlerPhase(), "_message=\"no dirty records found\"
		// handler_id={}", getId());
		// }
		// }
		setupFirstRun();
		setNextDescriptorToProcess();
		if (inputDescriptor == null) {
			logger.info(getHandlerPhase(),
					"_message=\"no descriptor to process, still returning READY\" handler_id={} ", getId());
			// return Status.BACKOFF;
		}
		// try {
		// setupConnection();
		// } catch (ClassNotFoundException | SQLException e) {
		// throw new HandlerException("unable to process", e);
		// }
		return Status.READY;
	}

	protected void setupFirstRun() throws RuntimeInfoStoreException {
		if (isFirstRun()) {
			dirtyRecords = getAllStartedRuntimeInfos(runtimeInfoStore, entityName);

			final Iterator<RuntimeInfo> dirtyRecordIter = dirtyRecords.iterator();
			while (dirtyRecordIter.hasNext()) {
				final RuntimeInfo dirtyRecord = dirtyRecordIter.next();

				if (!dirtyRecord.getInputDescriptor().startsWith(INPUT_DESCRIPTOR_PREFIX)) {
					logger.info(getHandlerPhase(),
							"_message=\"removing from dirty record list\" handler_id={} input_descriptor={} startWith={}",
							getId(), dirtyRecord.getInputDescriptor(),
							dirtyRecord.getInputDescriptor().startsWith(INPUT_DESCRIPTOR_PREFIX));
					dirtyRecordIter.remove();
				}
			}

			if (dirtyRecords != null && !dirtyRecords.isEmpty()) {
				dirtyRecordCount = dirtyRecords.size();
				logger.warn(getHandlerPhase(),
						"_message=\"dirty records found\" handler_id={} dirty_record_count=\"{}\" entityName={}",
						getId(), dirtyRecordCount, entityName);
			} else {
				logger.info(getHandlerPhase(), "_message=\"no dirty records found\" handler_id={}", getId());
			}
		}
	}

	protected void setNextDescriptorToProcess() throws IOException, RuntimeInfoStoreException, HandlerException {
		if (dirtyRecords != null && !dirtyRecords.isEmpty()) {
			RuntimeInfo dirtyRecord = dirtyRecords.remove(0);
			logger.info(getHandlerPhase(), "\"processing a dirty record\" dirtyRecord=\"{}\"", dirtyRecord);
			initRecordToProcess(dirtyRecord);
			// setHdfsOutputDirectory();
			processingDirty = true;
			return;
		} else {
			logger.info(getHandlerPhase(), "processing a clean record");
			processingDirty = false;
			RuntimeInfo queuedRecord = getOneQueuedRuntimeInfo(runtimeInfoStore, entityName, INPUT_DESCRIPTOR_PREFIX);
			logger.info(getHandlerPhase(), "queued_record={}", queuedRecord);
			if (queuedRecord == null) {
				boolean foundRecordsToProcess = findAndAddRuntimeInfoRecords();
				if (foundRecordsToProcess)
					queuedRecord = getOneQueuedRuntimeInfo(runtimeInfoStore, entityName, INPUT_DESCRIPTOR_PREFIX);
			}
			if (queuedRecord != null) {
				logger.info(getHandlerPhase(), "_message=\"found a queued record, will process this\" queued_record={}",
						queuedRecord);
				initRecordToProcess(queuedRecord);
				Map<String, String> properties = new HashMap<>();
				properties.put("handlerName", this.getClass().getName());
				updateRuntimeInfo(runtimeInfoStore, entityName, queuedRecord.getInputDescriptor(),
						RuntimeInfoStore.Status.STARTED, properties);
			} else {
				inputDescriptor = null;
			}
		}
	}

	protected void initRecordToProcess(RuntimeInfo runtimeInfo) throws HandlerException {
		Map<String, String> runtimeProperty = runtimeInfo.getProperties();
		final String hiveConfDate = runtimeProperty.get("hiveConfDate");
		outputDirectory = runtimeProperty.get("hiveConfDirectory");
		final String hiveQuery = runtimeProperty.get("hiveQuery");

		inputDescriptor = new HiveReaderDescriptor(entityName, hiveConfDate, outputDirectory, hiveQuery);

		hiveConfigurations.put("DIRECTORY", outputDirectory);
		hiveConfigurations.put("DATE", hiveConfDate);
		// setHdfsOutputDirectory();
		try {
			setupConnection();
		} catch (ClassNotFoundException | SQLException | IOException e) {
			throw new HandlerException("unable to process", e);
		}

	}

	private boolean findAndAddRuntimeInfoRecords() throws RuntimeInfoStoreException {
		long now = System.currentTimeMillis();
		if (hiveConfDateTime == 0) {// this is the first time
			hiveConfDateTime = now - goBackDays * 24 * 60 * 60 * 1000;
		} else if (now - hiveConfDateTime > intervalInMillis) {
			hiveConfDateTime = hiveConfDateTime + intervalInMillis;
		} else {
			logger.info("nothing to run", "now={} hiveConfDateTime={} intervalInMillis={}", now, hiveConfDateTime,
					intervalInMillis);
			return false;
		}
		setHdfsOutputDirectory();
		final HiveReaderDescriptor descriptor = new HiveReaderDescriptor(entityName, getHiveConfDate(), outputDirectory,
				hiveQuery);
		Map<String, String> properties = new HashMap<>();
		properties.put("hiveConfDate", getHiveConfDate());
		properties.put("hiveConfDirectory", outputDirectory);
		properties.put("hiveQuery", hiveQuery);

		queueRuntimeInfo(runtimeInfoStore, entityName, descriptor.getInputDescriptorString(), properties);

		return true;
	}

	private void setHdfsOutputDirectory() {
		if (!baseOutputDirectory.endsWith(FORWARD_SLASH))
			outputDirectory = baseOutputDirectory + FORWARD_SLASH;
		else
			outputDirectory = baseOutputDirectory;

		final String hiveConfDate = getHiveConfDate();
		final String hdfsOutputPathDate = getHdfsOutputPathDate();
		outputDirectory = outputDirectory + hdfsOutputPathDate + FORWARD_SLASH + entityName;

		logger.debug(getHandlerPhase(), "hiveConfDate={} hdfsOutputPathDate={} outputDirectory=\"{}\"", hiveConfDate,
				hdfsOutputPathDate, outputDirectory);

		hiveConfigurations.put("DIRECTORY", outputDirectory);
		hiveConfigurations.put("DATE", hiveConfDate);
	}

	/**
	 * Descriptor: entityName, date, outputDirectory, hiveQuery
	 * 
	 * tab, 20160724, dir, INSERT OVERWRITE DIRECTORY
	 * '${hiveconf:DIRECTORY}' SELECT tab.* FROM tab JOIN tab ON tab.fld =
	 * tab.fld AND tab.fld = tab.fld WHERE tab.fld IN (1,2,3,4) AND tab.fld >
	 * '${hiveconf:DATE}' DISTRIBUTE BY RAND()
	 * 
	 * @formatter:off
	 * if first run
	 *    check for dirty records
	 *    if dirty records found
	 *       process them
	 * 
	 * Get queued records
	 * if queued record found
	 *    process them
	 * else
	 *    generate queued record
	 *    get queued records
	 *    if queued record found
	 *      process them
	 *      return READY
	 *    else
	 *      return BACKOFF      
	 * @formatter:on
	 * 
	 */

	@Override
	public Status doProcess() throws HandlerException {

		logger.debug(getHandlerPhase(), "_messagge=\"entering doProcess\" invocation_count={}", getInvocationCount());
		try {
			ActionEvent outputEvent = new ActionEvent();
			if (inputDescriptor == null) {
				logger.debug(getHandlerPhase(),
						"returnung READY, so that next handler can process the pending records");
			} else {

				final Statement stmt = connection.createStatement();
				runHiveConfs(stmt);

				String jobName = "bigdime-dw" + "." + entityName + "." + ProcessHelper.getInstance().getProcessId()
						+ "." + jobDtf.print(System.currentTimeMillis());
				logger.debug(getHandlerPhase(), "hiveQuery=\"{}\" hiveConfigurations=\"{}\" jobName={}", hiveQuery,
						hiveConfigurations, jobName);
				// TODO: remove harcoding
				stmt.execute("set mapred.job.name=" + jobName);
				stmt.execute(hiveQuery);// no resultset is returned
				boolean updatedRuntime = updateRuntimeInfo(runtimeInfoStore, entityName,
						inputDescriptor.getInputDescriptorString(),
						io.bigdime.core.runtimeinfo.RuntimeInfoStore.Status.VALIDATED, outputEvent.getHeaders());
				logger.debug(getHandlerPhase(), "updatedRuntime={}", updatedRuntime);
			}
			outputEvent.getHeaders().put(ActionEventHeaderConstants.ENTITY_NAME, entityName);
			outputEvent.getHeaders().put(ActionEventHeaderConstants.HDFS_PATH, outputDirectory);
			outputEvent.getHeaders().put(ActionEventHeaderConstants.HiveJDBCReaderHeaders.HIVE_QUERY, hiveQuery);
			getHandlerContext().createSingleItemEventList(outputEvent);
			logger.debug(getHandlerPhase(), "completed process");
		} catch (final Exception e) {
			throw new HandlerException("unable to process", e);
		} finally {
			closeConnection();
		}

		return Status.READY;
	}

	public static final String FORWARD_SLASH = "/";

	private String getHiveConfDate() {
		final String hiveConfDate = hiveQueryDtf.print(hiveConfDateTime);
		return hiveConfDate;
	}

	private String getHdfsOutputPathDate() {
		final String hdfsOutputPathDate = hdfsOutputPathDtf.print(hiveConfDateTime);
		return hdfsOutputPathDate;
	}

	private void setupConnection() throws SQLException, IOException, ClassNotFoundException {
		if (connection == null) {
			if (authOption == HDFS_AUTH_OPTION.KERBEROS) {
				connection = hiveJdbcConnectionFactory.getConnectionWithKerberosAuthentication(driverClassName, jdbcUrl,
						userName, password, hiveConfigurations);
				logger.debug(getHandlerPhase(), "_message=\"connected to db\"");
			} else if (authOption == HDFS_AUTH_OPTION.PASSWORD) {
				connection = hiveJdbcConnectionFactory.getConnection(driverClassName, jdbcUrl, userName, password,
						hiveConfigurations);
				logger.debug(getHandlerPhase(), "_message=\"connected to db\"");
			}
		}
	}

	private void runHiveConfs(final Statement stmt) throws SQLException {
		if (hiveConfigurations != null) {
			for (final String prop : hiveConfigurations.keySet()) {
				String sql = "set " + prop + " = " + hiveConfigurations.get(prop);
				logger.debug(getHandlerPhase(), "running sql to set hiveconf: \"{}\"", sql);
				stmt.execute(sql);
			}
		}
	}

	private void closeConnection() {
		try {
			if (connection != null && !connection.isClosed()) {
				connection.close();
			}
		} catch (Exception e) {
			logger.warn(getHandlerPhase(), "_message=\"error while trying to close the connection\"", e);
		} finally {
			connection = null;
		}
	}

}
