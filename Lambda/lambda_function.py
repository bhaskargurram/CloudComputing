mport boto3
import json
import time

def lambda_handler(event, context):
    ssm_client = boto3.client('ssm')
    inputFileKey = event['s3InputKey']
    model_name = event['model_name']
    user_id = event['user_id']
    user_name = event['user_name']
    file_size = event['file_size']
    
    commands = ['cd /root; ./main.sh '+inputFileKey+' '+user_id+' '+user_name+' '+str(file_size)+' '+model_name]
    
    instance_id = 'Enter your instance id here'
    
    response = ssm_client.send_command( InstanceIds=[instance_id], DocumentName='AWS-RunShellScript', Parameters={ "commands":commands },  TimeoutSeconds=300000 )
    command_id = response['Command']['CommandId']
    output = get_command_results(ssm_client, command_id, instance_id)
    return output['StandardOutputContent'].strip()

    
def get_command_results(ssm_client, command_id, instance_id):
    # sleep time in seconds
    TIME_SLEEP = .9
    result = None
    try:
        while result is None or result['Status'] == 'InProgress':
            result = ssm_client.get_command_invocation(CommandId=command_id, InstanceId=instance_id)
            time.sleep(TIME_SLEEP)
        return result
    except:
        time.sleep(TIME_SLEEP)
        return get_command_results(ssm_client, command_id, instance_id)
        
        
