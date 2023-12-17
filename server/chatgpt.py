from openai import OpenAI
import json
import os
import random


client = OpenAI(
    #api_key="",
)

class MinecraftGPT:
    def __init__(self, model="gpt-4", target=None, targets={}):
        self.messages = []
        self.model = model
        self.targets = targets
        self.target = target if target in targets else random.choice(list(targets.keys()))

    def add_message(self, role, message):
        assert role in ["user", "assistant"]
        self.messages.append({"role": role, "content": message})

    def encode(self):
        return json.dumps({'messages': self.messages, 'target':self.target})

    @staticmethod
    def fromEncoded(encoded, targets):
        x = MinecraftGPT(targets=targets)
        data = json.loads(encoded)
        x.messages = data['messages']
        x.target = data['target']
        return x

    def target_data(self):
        return json.dumps(sorted(self.targets[self.target]))

    def ask(self, message=None):
        if message:
            self.add_message("user", message)
        setup_message = {   
            "role": "system",
            "content": f"You are an agent in a voxel world, where the most northernly point is 0,0,-5; the most westerly point -5,0,0; the most eastern point is 5,0,0; the most southern 0,0,5 and the y-axis is up and down, with y=0 being the minimum. Your task is to give instructions to a human to place blocks to achieve the target world state: {self.target_data()} where the target world instructions are in the format [[x,y,z,color],...].  Give easy to interpret instructions, do not directly mention the coordinates. The builder will respond with the coordinates of the blocks they have placed in the same format.  Don't ask for coordinates, they will always be given. Avoid long instructions with multiple steps and start building the structure from the ground up.",
        }
        messages = [setup_message] + self.messages
        print("sending", messages)
        chat_completion = client.chat.completions.create(
            messages=messages,
            #model="gpt-3.5-turbo",
            model=self.model,
        )
        response = chat_completion.choices[0].message.content
        self.add_message("assistant", response)
        return response

if __name__ == "__main__":
    mg = MinecraftGPT()
    print(mg.ask("hello"))
