package be.camunda.bpm;

import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import be.camunda.bpm.delegate.MyDelegate;
import be.camunda.bpm.domain.ProductDto;
import org.camunda.bpm.engine.TaskService;
import org.camunda.bpm.engine.runtime.ProcessInstance;
import org.camunda.bpm.engine.task.Task;
import org.camunda.bpm.engine.test.Deployment;
import org.camunda.bpm.engine.test.ProcessEngineRule;
import org.camunda.bpm.engine.test.mock.Mocks;
import org.camunda.bpm.engine.variable.VariableMap;
import org.camunda.bpm.engine.variable.Variables;
import org.joda.time.DateTime;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

/**
 * https://github.com/camunda/camunda-bpm-assert
 */
public class BpmValidationTest {

    @Rule
    public ProcessEngineRule processEngineRule = new ProcessEngineRule();

    private DateTime startTime;
    
    @Mock
    private MyDelegate myDelegate;

    @Before
    public void setUp() {
        startTime = getStartDateTime();
        processEngineRule.setCurrentTime(startTime.toDate());

        MockitoAnnotations.initMocks(this);
        Mocks.register("myDelegate", myDelegate);
    }

    @After
    public void tearDown() {
        Mocks.reset();
    }

    protected final void updateTaskVariables(TaskService taskService, String processInstanceId, String taskDefinitionKey, Map<String, Object> variables) {
        if (taskDefinitionKey != null && processInstanceId != null) {

            Task task = taskService.createTaskQuery().processInstanceId(processInstanceId).taskDefinitionKey(taskDefinitionKey).singleResult();
            taskService.setVariables(task.getId(), variables);
            System.out.println(String.format("variables set for Process Instance %s for Thread Id %s", processInstanceId, Thread.currentThread().getId()));
            taskService.saveTask(task);
            System.out.println(String.format("task saved for Process Instance %s for Thread Id %s", processInstanceId, Thread.currentThread().getId()));

        }
    }

    @Test
    @Deployment(resources = {"bpmn/varinst-error.bpmn"})
    public void testOneSingleRun()
    throws InterruptedException {

        System.out.println("STARTING SINGLE RUN");
        peformProcessSteps(processEngineRule, Thread.currentThread().getId());
        System.out.println("ENDING SINGLE RUN");

    }

    @Test
    @Deployment(resources = {"bpmn/varinst-error.bpmn"})
    public void testMultiThreadedRun()
    throws InterruptedException {

        System.out.println("STARTING MULTITHREADED RUN");

        for (int i = 0; i < 5; i++) {
            ExecutorService es = Executors.newSingleThreadExecutor();
            es.execute(new Runnable() {
                @Override
                public void run() {
                    try {
                        System.out.println("Starting Thread Id: " + Thread.currentThread().getId());
                        peformProcessSteps(processEngineRule, Thread.currentThread().getId());
                        System.out.println("Closing Thread Id: " + Thread.currentThread().getId());

                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            });
            es.shutdown();
        }

        System.out.println("ENDING MULTITHREADED RUN");

        // This is in order to avoid process definition undeploy from TearDown.
        Thread.sleep(5000L);
    }

    private void peformProcessSteps(ProcessEngineRule processEngineRule, Long threadId) {

        ProductDto productDto = new ProductDto();
        String varString = "hello " + threadId;
        Long varLong = 100L;

        VariableMap startVarMap = Variables.createVariables()
            .putValue("varString", varString)
            .putValue("varLong", varLong)
            .putValue("productJson", Variables.objectValue(productDto)
                .serializationDataFormat(Variables.SerializationDataFormats.JSON).create())
            .putValue("productJava", Variables.objectValue(productDto)
                .serializationDataFormat(Variables.SerializationDataFormats.JAVA)
                .create());

        ProcessInstance processInstance = processEngineRule.getRuntimeService()
            .startProcessInstanceByKey("VarInstError", startVarMap);

        System.out.println(String.format("started Process Instance %s for Thread Id %s", processInstance.getProcessInstanceId(), Thread.currentThread().getId()));

        //        ProcessInstance processInstance = ProcessEngineTests.runtimeService()
        //            .startProcessInstanceByKey("VarInstError", startVarMap);

        String taskDefinitionKey = "UserTask";
        updateTaskVariables(processEngineRule.getTaskService(), processInstance.getProcessInstanceId(), taskDefinitionKey, startVarMap);
        updateTaskVariables(processEngineRule.getTaskService(), processInstance.getProcessInstanceId(), taskDefinitionKey, startVarMap);
        updateTaskVariables(processEngineRule.getTaskService(), processInstance.getProcessInstanceId(), taskDefinitionKey, startVarMap);

        Task taskToComplete = processEngineRule.getTaskService().createTaskQuery().processInstanceId(processInstance.getProcessInstanceId()).taskDefinitionKey(taskDefinitionKey).singleResult();
        processEngineRule.getTaskService().complete(taskToComplete.getId(), startVarMap);
        System.out.println(String.format("completed Task for Process Instance %s for Thread Id %s", processInstance.getProcessInstanceId(), Thread.currentThread().getId()));

    }

    private DateTime getStartDateTime() {
        return new DateTime()
            .withHourOfDay(23)
            .withMinuteOfHour(59)
            .withSecondOfMinute(59)
            .withMillisOfSecond(0);
    }

}
