package be.camunda.bpm.delegate;

import javax.ejb.Stateless;
import javax.inject.Named;

import org.camunda.bpm.engine.delegate.DelegateExecution;
import org.camunda.bpm.engine.delegate.JavaDelegate;

@Stateless
@Named("myDelegate")
public class MyDelegate implements JavaDelegate {

    @Override
    public void execute(DelegateExecution delegateExecution)
    throws Exception {
    }

}
