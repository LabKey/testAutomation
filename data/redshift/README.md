#Redshift Sample Data Restore Instructions

If the Redshift Database Cluster needs to be recreated with its sample data, please follow the steps below:

1. Review the two tutorials to get an idea of what is going to be done:

    https://docs.aws.amazon.com/redshift/latest/gsg/getting-started.html
    https://docs.aws.amazon.com/redshift/latest/dg/tutorial-loading-data.html
    
2. Access the AWS Console for lk-teamcity and create an IAM role for Redshift to read S3 data or you can generate a temporary AWS Access Key to use to
access S3.

    *Note: It may be easier just to create the access key and then delete it when finished.*

3. Create the Redshift Cluster as described in the tutorials. Be sure to setup the node as a dc2.large, but use the
following database configurations:

    - Database name: lkcluster
    - Database port: 5439 (this is the default for Redshift)
    - Master user name: lkredshiftadmin
    
    Refer to LastPass for the existing password or create a new one and keep that safe.
    
4. Authorize access to the cluster as described in
[step 4](https://docs.aws.amazon.com/redshift/latest/gsg/rs-gsg-authorize-cluster-access.html). The cluster needs to be
accessible outside of the lk-teamcity AWS account. Please review your existing EC2-VPC security groups to assign an
appropriate group that will allow access.

5. Verify if you can connect to the new Redshift cluster using psql from your computer. This will confirm
whether the security group configuration in Step 4 was done properly.

6. Instead of using the sample data from the tutorial, you will want to create the schema and tables
using the files in this redshift data folder.

    Use the create-redshift-tables.sql file to create the schema and the four tables via psql.

7. The current S3 bucket that was originally made was s3://lk-test-redshift-cluster-1 and contains the four TSV files
that are in this same directory. If the S3 bucket and/or the files do not exist in S3, you will need to make a new
bucket or use the files above to add them back into the bucket.

    *Note: The bucket and objects do not need to be public.*

8. Use the following syntax to perform a COPY from the S3 bucket into the Redshift cluster using psql:

    If using an AccessKey/SecretKey combo:

    ```
    copy schemaname.tablename from 's3://bucketname/foldername/filename.tsv'
    credentials 'aws_access_key_id=YOUR-ACCESS-KEY-ID;aws_secret_access_key=YOUR-SECRET-ACCESS-KEY'
    DELIMITER '\t'
    DATEFORMAT 'YYYY-MM-DD HH:MI:SS';
    ```

    If using an IAM role:
    ```
    copy schemaname.tablename from 's3://bucketname/foldername/filename.tsv'
    credentials 'aws_iam_role=IAM-ROLE-ARN'
    DELIMITER '\t'
    DATEFORMAT 'YYYY-MM-DD HH:MI:SS';
    ```

    Repeat the above copy for each file. Verify the data loaded correctly by running a SELECT on the tables.

