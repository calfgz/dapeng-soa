package com.github.dapeng.soa.scala

        import com.github.dapeng.core._;
        import com.github.dapeng.org.apache.thrift._;
        import java.util.ServiceLoader;
        import com.github.dapeng.soa.scala.PrintServiceCodec._;
        import com.github.dapeng.soa.scala.service.PrintService;

        /**
         * Autogenerated by Dapeng-Code-Generator (2.1.1-SNAPSHOT)
 *
 * DO NOT EDIT UNLESS YOU ARE SURE THAT YOU KNOW WHAT YOU ARE DOING
 *  @generated

        **/
        class PrintServiceClient extends PrintService {

        import java.util.function.{ Function ⇒ JFunction, Predicate ⇒ JPredicate, BiPredicate }
        implicit def toJavaFunction[A, B](f: Function1[A, B]) = new JFunction[A, B] {
          override def apply(a: A): B = f(a)
        }

        val serviceName = "com.github.dapeng.soa.service.PrintService"
        val version = "1.0.0"
        val pool = {
          val serviceLoader = ServiceLoader.load(classOf[SoaConnectionPoolFactory])
          if (serviceLoader.iterator().hasNext) {
            val poolImpl = serviceLoader.iterator().next().getPool
            poolImpl.registerClientInfo(serviceName,version)
            poolImpl
          } else null
        }

        def getServiceMetadata: String = {
          pool.send(
          serviceName,
          version,
          "getServiceMetadata",
          new getServiceMetadata_args,
          new GetServiceMetadata_argsSerializer,
          new GetServiceMetadata_resultSerializer
          ).success
        }


        
            /**
            * 
            **/
            def print() : Unit = {

            val response = pool.send(
            serviceName,
            version,
            "print",
            print_args(),
            new Print_argsSerializer(),
            new Print_resultSerializer())

            

          }
          
            /**
            * 
            **/
            def printInfo(info:com.github.dapeng.soa.scala.domain.Info ) : String = {

            val response = pool.send(
            serviceName,
            version,
            "printInfo",
            printInfo_args(info),
            new PrintInfo_argsSerializer(),
            new PrintInfo_resultSerializer())

            response.success

          }
          
            /**
            * 
            **/
            def printInfo2(name:String ) : String = {

            val response = pool.send(
            serviceName,
            version,
            "printInfo2",
            printInfo2_args(name),
            new PrintInfo2_argsSerializer(),
            new PrintInfo2_resultSerializer())

            response.success

          }
          
            /**
            * 
            **/
            def printInfo3() : String = {

            val response = pool.send(
            serviceName,
            version,
            "printInfo3",
            printInfo3_args(),
            new PrintInfo3_argsSerializer(),
            new PrintInfo3_resultSerializer())

            response.success

          }
          
      }
      