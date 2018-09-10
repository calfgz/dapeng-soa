package com.isuwang.soa.user;

      import com.github.dapeng.core.*;
      import com.github.dapeng.org.apache.thrift.*;
      import java.util.concurrent.CompletableFuture;
      import java.util.concurrent.Future;
      import java.util.ServiceLoader;
      import com.isuwang.soa.user.UserServiceAsyncCodec.*;
      import com.isuwang.soa.user.service.UserServiceAsync;

      /**
       * Autogenerated by Dapeng-Code-Generator (2.0.5)
 *
 * DO NOT EDIT UNLESS YOU ARE SURE THAT YOU KNOW WHAT YOU ARE DOING
 *  @generated

      **/
      public class UserServiceAsyncClient implements UserServiceAsync{
      private final String serviceName;
      private final String version;

      private SoaConnectionPool pool;

      public UserServiceAsyncClient() {
        this.serviceName = "com.isuwang.soa.user.service.UserService";
        this.version = "1.0.0";

        ServiceLoader<SoaConnectionPoolFactory> factories = ServiceLoader.load(SoaConnectionPoolFactory.class);
        for (SoaConnectionPoolFactory factory: factories) {
          this.pool = factory.getPool();
          break;
        }
        this.pool.registerClientInfo(serviceName,version);
      }

      
          
            /**
            * 
            **/
            
              public CompletableFuture<Void> createUser(com.isuwang.soa.user.domain.User user) throws SoaException{

              String methodName = "createUser";
              createUser_args createUser_args = new createUser_args();
              createUser_args.setUser(user);
                

              CompletableFuture<createUser_result> response = (CompletableFuture<createUser_result>) pool.sendAsync(serviceName,version,"createUser",createUser_args, new CreateUser_argsSerializer(), new CreateUser_resultSerializer());

              
                  return response.thenApply((createUser_result result )->  null);
                
            }
            
          

        
          
            /**
            * 
            **/
            
              public CompletableFuture<com.isuwang.soa.user.domain.User> getUserById(Integer userId) throws SoaException{

              String methodName = "getUserById";
              getUserById_args getUserById_args = new getUserById_args();
              getUserById_args.setUserId(userId);
                

              CompletableFuture<getUserById_result> response = (CompletableFuture<getUserById_result>) pool.sendAsync(serviceName,version,"getUserById",getUserById_args, new GetUserById_argsSerializer(), new GetUserById_resultSerializer());

              
                  
                      return response.thenApply((getUserById_result result )->  result.getSuccess());
                    
                
            }
            
          

        

      /**
      * getServiceMetadata
      **/
      public String getServiceMetadata() throws SoaException {
        String methodName = "getServiceMetadata";
        getServiceMetadata_args getServiceMetadata_args = new getServiceMetadata_args();
        getServiceMetadata_result response = pool.send(serviceName,version,methodName,getServiceMetadata_args, new GetServiceMetadata_argsSerializer(), new GetServiceMetadata_resultSerializer());
        return response.getSuccess();
      }

    }
    