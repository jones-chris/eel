package io.eel.flow_api_core.service;

import io.eel.common.model.Flow;
import io.eel.flow_api_core.dao.FlowDao;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.Optional;
import java.util.Set;
import java.util.UUID;

public class FlowServiceImpl implements FlowService {

    private static final Logger log = LogManager.getLogger(FlowServiceImpl.class);
    private FlowDao flowDao;

    private FlowServiceImpl() {}

    public FlowServiceImpl(FlowDao flowDao) {
        this.flowDao = flowDao;
    }

    @Override
    public Flow createNewFlow() {
        // Create new empty flow to generate UUID.
        return this.flowDao.createNewFlow("chris.jones"); // todo:  change the author parameter

//        try {
//            InputStream inputStream = new FileInputStream()
//            File outputFile = new File("output.txt");
//            FileUtils.copyInputStreamToFile(inputStream, outputFile);
//        } catch (Throwable t) {
//            throw new RuntimeException(t);
//        }

//        try {
//            File file = File.createTempFile("test", ".csv");
////            URL url = this.getClass().getResource("/test.csv");
////            assert url != null;
////            Path path = Paths.get(url.toURI());
//
//            this.flowDao.saveFileToS3(file.toPath());
//        } catch (Throwable t) {
//            throw new RuntimeException(t);
//        }

        // Generate presigned URL.
//        return new FlowInitDto(flow.getId(), flow.getVersion(), flow.getTransformationStagingUrl());
    }

    @Override
    public Flow updateFlow(Flow flow) {
        final Flow newFlowVersion = flow.increment();

        return this.flowDao.updateFlow(newFlowVersion);
    }

    @Override
    public Optional<Flow> getFlowById(UUID id) {
        return this.flowDao.getFlowById(id);
    }

    @Override
    public Set<UUID> getFlowsByUser(String userName) {
        return this.flowDao.getFlowsByUser(userName);
    }

    @Override
    public String generateTransformationStagingPresignedUrl(UUID flowId) {
        return this.flowDao.generateTransformationStagingPresignedUrl(flowId);
    }
}
