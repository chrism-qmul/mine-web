from gevent import monkey
monkey.patch_all()
from flask import Flask, render_template
from flask_socketio import SocketIO, send, emit
import json
import os
from celery import Celery
import random

app = Flask(__name__)
app.config['SECRET_KEY'] = 'secret!!'
socketio = SocketIO(app)

celery = Celery('tasks', broker='redis://209.97.129.93/0', backend='redis://209.97.129.93/0')

#celery.config_from_object('celeryconfig')
def sample(dialog, actions):
    task = celery.send_task('tasks.learn_to_ask', args=[dialog, actions])
    result = task.get()
    app.logger.info(f"result {str(result)}")
    return result

def random_blocks():
    rand_coord = lambda: random.randint(-5,5)
    rand_color = lambda: random.randint(1, 6)
    return [[[rand_coord(), 1, rand_coord()], rand_color()] for _ in range(3)]

@socketio.on('update-game')
def handle_json(message):
    message = json.loads(message)
    #world_state = [(tuple(world), color, "putdown") for world, color in message['world-state']]
    world_state = [(tuple(world), color, action) for world, color, action in message['actions']]
    app.logger.info('[input] world:state' + str(world_state) + "; dialog: " + str(message['dialog']))
    result = sample(message['dialog'], world_state)
    app.logger.info('result: ' + str(result))
    #emit("update-game", {'world-state': [[[1,63,1],86],[[4,63,4],86],[[3,63,3],86]]})
    emit("update-game", {'actions': [[coord, color, action] for coord, color, action in result]})

@socketio.on('connect')
def test_connect(auth):
    pass
    #emit("update-game", json.dumps({'data': 'Connected'}))

@socketio.on('disconnect')
def test_disconnect():
    print('Client disconnected')

if __name__ == '__main__':
    os.environ['FLASK_ENV'] = 'development'
    print("posting task as test")
    result = celery.send_task('tasks.learn_to_ask', args=(["hello"], []))
    print("test result", result.get())
    socketio.run(app, host="0.0.0.0", debug=True, allow_unsafe_werkzeug=True)
