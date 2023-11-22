from gevent import monkey
#from gevent.events import GeventWillPatchModuleEvent, GeventWillPatchAllEvent
#import zope.event.classhandler
#import sys
#print(sys.path)
#@zope.event.classhandler.handler(GeventWillPatchModuleEvent)
#def gevent_patch_listen(x):
#    print("patching", x.source.__name__)
try:
    monkey.patch_all()
except:
    # expected, monkey.patch_all will touch modules in openai that don't need
    # to be imported. can safely ignore
    pass
#import openai
from flask import Flask, render_template, request, session
from flask_session import Session
from flask_socketio import SocketIO, send, emit
from uuid import uuid4
import json
import os
from celery import Celery
import random
import redis
REDIS_HOST = os.getenv("REDIS_HOST", "localhost")
app = Flask(__name__)
app.config['SESSION_REDIS'] = redis.Redis(REDIS_HOST)
app.config['SECRET_KEY'] = 'secret!!'
s = Session()
s.init_app(app)
socketio = SocketIO(app)

task_routes = {
    'tasks.learn_to_ask': 'learn_to_ask',
    'tasks.collaborative': 'collaborative',
}

celery = Celery('tasks', broker='redis://209.97.129.93/0',  backend='redis://209.97.129.93/0', task_routes=task_routes)

#celery.config_from_object('celeryconfig')
def sample(dialog, actions, model):
    task_name = f"tasks.{model}"
    app.logger.info(f"[{task_name}]: {str(dialog)} {str(actions)}")
    task = celery.send_task(task_name, args=[dialog, actions])#, soft_time_limit=10, queue=model)
    result = task.get()
    app.logger.info(f"[{task_name}] : result {str(result)}")
    return result

def random_blocks():
    rand_coord = lambda: random.randint(-5,5)
    rand_color = lambda: random.randint(1, 6)
    return [[[rand_coord(), 1, rand_coord()], rand_color()] for _ in range(3)]

@socketio.on('update-game')
def handle_json(message):
    #import chatgpt
    from chatgpt import MinecraftGPT
    message = json.loads(message)
    agent = MinecraftGPT.fromEncoded(session['agent']) if session['agent'] else MinecraftGPT()
    agent_reply = agent.ask(message['dialog'][-1])
    session['agent'] = agent.encode()
    with open(f"/data/{session['uuid']}.txt", "a+") as fh:
        fh.write(message['dialog'][-1] + "\n")
        fh.write(json.dumps(agent_reply) + "\n")
    #print("dialog", message['dialog'])
    #new_world = chatgpt.minecraft(message['dialog'])
    #world_state = [(tuple(world), color, "putdown") for world, color in message['world-state']]
    print("new_world", agent_reply)
    #ymin = 0
    #if new_world:
    #    ymin = abs(min(0, *[y for x, y, z, color in new_world]))
    add = []
    remove = []
    try:
        add = [[[x,y,z], color, "putdown"] for x, y, z, color in agent_reply.get("add")]
        remove = [[[x,y,z], color, "pickup"] for x, y, z, color in agent_reply.get("remove")]
    except:
        pass
    actions = add + remove
    #world_state = [((x,y+ymin,z), color, "putdown" if action=="add" else "pickup") for x, y, z, color, action in new_world]
#    app.logger.info('[input] world:state' + str(world_state) + "; dialog: " + str(message['dialog']))
#    result = sample(message['dialog'], world_state, message['model'])
    #app.logger.info('result: ' + str(result))
    #emit("update-game", {'world-state': [[[1,63,1],86],[[4,63,4],86],[[3,63,3],86]]})
    emit("update-game", {'confidence': agent_reply.get('confidence', 1.0), 'actions': actions, 'question': agent_reply.get("question","")})

#@socketio.on('update-game')
#def handle_json(message):
#    message = json.loads(message)
#    #world_state = [(tuple(world), color, "putdown") for world, color in message['world-state']]
#    world_state = [(tuple(world), color, action) for world, color, action in message['actions']]
#    app.logger.info('[input] world:state' + str(world_state) + "; dialog: " + str(message['dialog']))
#    result = sample(message['dialog'], world_state, message['model'])
#    app.logger.info('result: ' + str(result))
#    #emit("update-game", {'world-state': [[[1,63,1],86],[[4,63,4],86],[[3,63,3],86]]})
#    emit("update-game", {'actions': [[coord, color, action] for coord, color, action in result]})
#

@socketio.on('connect')
def test_connect(auth):
    session['agent'] = None
    session['uuid'] = str(uuid4())
    #emit("update-game", json.dumps({'data': 'Connected'}))

@socketio.on('disconnect')
def test_disconnect():
    print('Client disconnected')

if __name__ == '__main__':
    os.environ['FLASK_ENV'] = 'development'
    print("not posting task as test")
    #print(chatgpt.minecraft("make a tower of 3 yellow blocks"))
    #result = sample(["hello"], [], "collaborative")
    #result = celery.send_task('tasks.collaborative', soft_time_limit=10, args=(["hello"], []))#, queue="learn_to_ask")
    #print("test result", result)
    socketio.run(app, host="0.0.0.0", debug=True, allow_unsafe_werkzeug=True)
