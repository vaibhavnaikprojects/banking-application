package com.zensar.banking.bankingservices;
import java.util.ArrayList;
import java.util.HashMap;
import com.zensar.banking.bankingdaoservices.BankingDAOServices;
import com.zensar.banking.bankingdaoservices.CollectionBankingDAOServicesImpl;
import com.zensar.banking.beans.*;
import com.zensar.banking.exceptions.*;
import com.zensar.banking.providers.BankingServiceDAOProvider;

public class HDFCBankingServicesImpl implements BankingServices {
	BankingDAOServices daoServicesImpl;
	public HDFCBankingServicesImpl() throws ServicesNotFoundException {
		daoServicesImpl=BankingServiceDAOProvider.daoServices();
	}
	public int acceptCustomerDetails(String custName, String homeAddressCity,String homeAddressState, int homeAddressPincode,String localAddressCity, String localAddressState,int localAddressPincode) throws InvalidPincodeException,ServicesNotFoundException {
		if((homeAddressPincode+"").length()!=6||((localAddressPincode+"").length()!=6)) throw new InvalidPincodeException("INVALID PINCODE");
		return daoServicesImpl.insertCustomer(new Customer(custName,new Address(homeAddressCity,homeAddressState,homeAddressPincode),new Address(localAddressCity,localAddressState,localAddressPincode)));
	}
	public int openAccount(int custId, int balance, String accType)throws InvalidCustomerIdException, CustomerNotFoundException,InvalidAmountException, InvalidAccountTypeException,ServicesNotFoundException, NumberOFAccountsExceededException {
		@SuppressWarnings("unused")
		Customer customer=getCustomerDetails(custId);
		if(accType==null) throw new InvalidAccountTypeException("account type invalid");
		else if(balance<0) throw new InvalidAmountException("AMOUNT MUST BE GREATER THAN ZERO");
		return daoServicesImpl.insertAccount(custId,new Account(balance,accType));
	}
	public int withdraw(int custId, int accNo,int pin, int amt)throws InvalidCustomerIdException, CustomerNotFoundException,InvalidAccountNoException, AccountNotFoundException,InvalidAmountException, InsufficientBalanceException,ServicesNotFoundException, IncorrectPinException, AccountBlockException {
		Account account=getAccountDetails(custId, accNo,pin);
		if(account.getStatus()=="BLOCKED") throw new AccountBlockException("ACCOUNT BLOCKED,REGENERATE PIN");
		if(amt<0) throw new InvalidAmountException("AMOUNT IS LESS THAN ZERO");
		if(account.getBalance()<amt) throw new InsufficientBalanceException("INSUFFICIENT BALANCE");
		account.setBalance(account.getBalance()-amt);
		daoServicesImpl.updateAccount(custId, account);
		daoServicesImpl.insertTransaction(custId, accNo,new Transaction(0,amt,"withdraw"));
		return CollectionBankingDAOServicesImpl.getTransactionId()-1;
	}
	public int withdrawWithMemory(int custId, int accNo,int pin,int option) throws InvalidCustomerIdException, CustomerNotFoundException,InvalidAccountNoException, AccountNotFoundException,InvalidAmountException, InsufficientBalanceException,ServicesNotFoundException, IncorrectPinException, AccountBlockException, EnterCorrectionOptionException, NoMemoryWithdrawlException {
		int transactionId=0;
		Account account=getAccountDetails(custId, accNo,pin);
		if(option!=1&&option!=0) throw new EnterCorrectionOptionException("Enter Correct option 1:=yes/2:=no");
		if(option==1) if(account.getWithdrawMemBal()==0) throw new NoMemoryWithdrawlException("NO WITHDRAWL MEMORY FOUND");
		else transactionId=withdraw(custId,accNo,pin,account.getWithdrawMemBal());
		return transactionId;
	}
	public boolean fundTransfer(int custIdFrom, int accNoFrom,int pinX, int custIdTo,int accNoTo, int amt) throws InvalidCustomerIdException,CustomerNotFoundException, InvalidAccountNoException,AccountNotFoundException, InvalidAmountException,InsufficientBalanceException, ServicesNotFoundException, SameAccountException, IncorrectPinException, AccountBlockException {
		Account accountX=getAccountDetails(custIdFrom, accNoFrom,pinX);
		Account accountY=getAccountDetails(custIdTo, accNoTo);
		if(accountX.getStatus()=="BLOCKED") throw new AccountBlockException("ACCOUNT BLOCKED,REGENERATE PIN");
		if(accNoFrom==accNoTo) throw new SameAccountException("Account cannot be same for Fund Transfer");
		if(amt<0) throw new InvalidAmountException("AMOUNT IS LESS THAN ZERO");
		if(accountX.getBalance()<amt) throw new InsufficientBalanceException("INSUFFICIENT BALANCE");
		accountX.setBalance(accountX.getBalance()-amt);
		daoServicesImpl.updateAccount(custIdFrom, accountX);
		daoServicesImpl.insertTransaction(custIdFrom, accNoFrom,new Transaction(0,amt,"fundWithdraw"));
		accountY.setBalance(accountY.getBalance()+amt);
		daoServicesImpl.updateAccount(custIdTo, accountY);
		daoServicesImpl.insertTransaction(custIdTo, accNoTo,new Transaction(0,amt,"fundDeposit"));
		return true;
	}
	public int deposit(int custId, int accNo,int pin, int amt)throws InvalidCustomerIdException, CustomerNotFoundException,InvalidAccountNoException, AccountNotFoundException,InvalidAmountException, ServicesNotFoundException, IncorrectPinException, AccountBlockException {
		Account account=getAccountDetails(custId, accNo,pin);
		if(account.getStatus()=="BLOCKED") throw new AccountBlockException("ACCOUNT BLOCKED,REGENERATE PIN");
		if(amt<0) throw new InvalidAmountException("AMOUNT IS LESS THAN ZERO");
		account.setBalance(account.getBalance()+amt);
		daoServicesImpl.updateAccount(custId, account);
		daoServicesImpl.insertTransaction(custId, accNo,new Transaction(0,amt,"deposit"));
		return CollectionBankingDAOServicesImpl.getTransactionId()-1;
	}
	public boolean deleteCustomer(int customerId)throws CustomerNotFoundException, InvalidCustomerIdException,ServicesNotFoundException {
		Customer customer=getCustomerDetails(customerId);
		return daoServicesImpl.deleteCustomer(customer.getCustId());
	}
	public boolean deleteCustomerAccount(int customerId, int accountNo,int pin)throws CustomerNotFoundException, InvalidCustomerIdException,InvalidAccountNoException, AccountNotFoundException,ServicesNotFoundException, IncorrectPinException, AccountBlockException {
		return daoServicesImpl.deleteAccount(customerId, getAccountDetails(customerId,accountNo,pin).getAccNo());
	}
	public int getAccountBalance(int custId, int accNo,int pin)throws CustomerNotFoundException, InvalidCustomerIdException,InvalidAccountNoException, AccountNotFoundException,ServicesNotFoundException, IncorrectPinException, AccountBlockException {
		return getAccountDetails(custId, accNo,pin).getBalance();
	}
	public Customer getCustomerDetails(int custId)throws InvalidCustomerIdException, CustomerNotFoundException,ServicesNotFoundException {
		if((custId+"").length()<3) throw new InvalidCustomerIdException("Inavalid customer id");
		Customer customer=daoServicesImpl.getCustomer(custId);
		if(customer==null) throw new CustomerNotFoundException("CUSTOMER NOT FOUND");
		return customer;
	}
	public Account getAccountDetails(int custId, int accNo,int pin)throws InvalidCustomerIdException, CustomerNotFoundException,InvalidAccountNoException, AccountNotFoundException,ServicesNotFoundException, IncorrectPinException, AccountBlockException {
		if((accNo+"").length()<4) throw new InvalidAccountNoException("INVALID ACCOUNT NO");
		Account account=getCustomerDetails(custId).getAccounts().get(accNo);
		if(account==null)	throw new AccountNotFoundException("ACCOUNT NOT FOUND");
		else if(account.getPin()!=pin){
			account.setCounter(1+account.getCounter());
			if(account.getCounter()>2){
				account.setStatus("BLOCKED");
				throw new AccountBlockException("ACCOUNT BLOCKED");
			}
			throw new IncorrectPinException("INCORRECT PIN");	
		}
		account.setCounter(0);
		return account;
	}
	public Account getAccountDetails(int custId, int accNo)throws InvalidCustomerIdException, CustomerNotFoundException,InvalidAccountNoException, AccountNotFoundException,ServicesNotFoundException {
		if((accNo+"").length()<4) throw new InvalidAccountNoException("INVALID ACCOUNT NO");
		Account account=getCustomerDetails(custId).getAccounts().get(accNo);
		if(account==null)	throw new AccountNotFoundException("ACCOUNT NOT FOUND");
		return account;
	}	
	public HashMap<Integer,Account> getAllAccountsDetails(int custId)throws InvalidCustomerIdException, CustomerNotFoundException,ServicesNotFoundException {
		return getCustomerDetails(custId).getAccounts();
	}
	public ArrayList<Transaction> getAllTransactionDetails(int custId, int accNo,int pin)throws InvalidCustomerIdException, CustomerNotFoundException,InvalidAccountNoException, AccountNotFoundException,ServicesNotFoundException, IncorrectPinException, AccountBlockException {
		return getAccountDetails(custId, accNo,pin).getTransaction();
	}
	public boolean closeBankingServices() throws ServicesNotFoundException {
		return daoServicesImpl.closeBankingDaoServices();
	}
	public int regeneratePin(int custId, int accNo)throws InvalidCustomerIdException, CustomerNotFoundException,InvalidAccountNoException, AccountNotFoundException,ServicesNotFoundException {
		Account account=getAccountDetails(custId, accNo);
		account.setPin(daoServicesImpl.generatePin());
		account.setStatus("ENABLED");
		daoServicesImpl.updateAccount(custId, account);
		return account.getPin();
	}
	public int changePin(int custId,int accNo,int pin,int newPin,int cPin) throws PinNotMatchException, InvalidCustomerIdException, CustomerNotFoundException, InvalidAccountNoException, AccountNotFoundException, ServicesNotFoundException, IncorrectPinException, AccountBlockException, InvalidPinCountException{
		Account account=getAccountDetails(custId, accNo, pin);
		if((newPin+"").length()!=4||(cPin+"").length()!=4)	throw new InvalidPinCountException("PIN SHOULD BE OF LENGTH 4");
		if(newPin!=cPin)	throw new PinNotMatchException("PIN DOES NOT MATCH");
		account.setPin(newPin);
		daoServicesImpl.updateAccount(custId, account);
		return account.getPin();
	}
	public void setWithdrawlFavourite(int custId,int accNo,int amount) throws InvalidCustomerIdException, CustomerNotFoundException, InvalidAccountNoException, AccountNotFoundException, ServicesNotFoundException{
		Account account=getAccountDetails(custId, accNo);
		account.setWithdrawMemBal(amount);
		daoServicesImpl.updateAccount(custId, account);
	}
}